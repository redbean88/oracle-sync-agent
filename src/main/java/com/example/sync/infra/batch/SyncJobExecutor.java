package com.example.sync.infra.batch;

import com.example.sync.infra.checkpoint.CheckpointService;
import com.example.sync.infra.exception.PermanentSyncException;
import com.example.sync.infra.exception.TransientSyncException;
import com.example.sync.infra.monitoring.LagMonitor;
import com.example.sync.infra.monitoring.SyncMetrics;
import com.example.sync.repository.reader.SourceDataReader;
import com.example.sync.domain.dto.SourceRecordDto;
import com.example.sync.infra.retry.RetryQueueProcessor;
import com.example.sync.repository.writer.TargetDataWriter;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SyncJobExecutor {

    private static final Logger log = LoggerFactory.getLogger(SyncJobExecutor.class);

    private final CheckpointService checkpointService;
    private final SourceDataReader reader;
    private final TargetDataWriter writer;
    private final AdaptiveChunkSizer chunkSizer;
    private final LagMonitor lagMonitor;
    private final RetryQueueProcessor retryProcessor;
    private final SyncMetrics metrics;
    private volatile boolean shuttingDown = false;

    public SyncJobExecutor(CheckpointService checkpointService, SourceDataReader reader, TargetDataWriter writer, 
                           AdaptiveChunkSizer chunkSizer, LagMonitor lagMonitor, 
                           RetryQueueProcessor retryProcessor, SyncMetrics metrics) {
        this.checkpointService = checkpointService;
        this.reader = reader;
        this.writer = writer;
        this.chunkSizer = chunkSizer;
        this.lagMonitor = lagMonitor;
        this.retryProcessor = retryProcessor;
        this.metrics = metrics;
    }

    @PreDestroy
    public void shutdown() {
        log.info("[SyncJob] Shutdown 시그널 수신 - 현재 작업 완료 후 종료 예정");
        this.shuttingDown = true;
    }

    public void execute() {
        if (shuttingDown) {
            log.warn("[SyncJob] Shutdown 진행 중이므로 새로운 작업을 시작하지 않습니다.");
            return;
        }
        String jobName = "ORDERS_SYNC";
        long lastId = checkpointService.getLastId(jobName);
        int chunkSize = chunkSizer.currentSize();

        long readStart = System.currentTimeMillis();
        List<SourceRecordDto> records;
        try {
            records = reader.fetchChunk(lastId, chunkSize);
        } catch (Exception e) {
            log.error("[SyncJob] Read 실패 lastId={}", lastId, e);
            metrics.incrementReadError();
            return;
        }
        long readMs = System.currentTimeMillis() - readStart;
        log.info("[SyncJob] Read {} 건 / {}ms", records.size(), readMs);
        metrics.recordReadTime(readMs);

        if (records.isEmpty()) {
            log.info("[SyncJob] 처리할 데이터 없음 → skip");
            return;
        }

        long writeStart = System.currentTimeMillis();
        try {
            writer.bulkUpsert(records);
        } catch (TransientSyncException e) {
            retryProcessor.enqueue(records, e);
            return;
        } catch (PermanentSyncException e) {
            log.error("[SyncJob] 영구 오류 — DLQ 이동", e);
            retryProcessor.enqueue(records, e);
            return;
        }
        long writeMs = System.currentTimeMillis() - writeStart;
        log.info("[SyncJob] Write {} 건 / {}ms", records.size(), writeMs);
        metrics.recordWriteTime(writeMs);

        chunkSizer.adjust(writeMs);

        long newLastId = records.get(records.size() - 1).getId();
        checkpointService.update(jobName, newLastId, records.size());

        lagMonitor.check(jobName, newLastId);
        retryProcessor.processQueue();
    }
}
