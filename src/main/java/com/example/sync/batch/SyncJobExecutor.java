package com.example.sync.batch;

import com.example.sync.checkpoint.CheckpointService;
import com.example.sync.exception.PermanentSyncException;
import com.example.sync.exception.TransientSyncException;
import com.example.sync.monitoring.LagMonitor;
import com.example.sync.monitoring.SyncMetrics;
import com.example.sync.reader.SourceDataReader;
import com.example.sync.reader.dto.SourceRecordDto;
import com.example.sync.retry.RetryQueueProcessor;
import com.example.sync.writer.TargetDataWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SyncJobExecutor {

    private final CheckpointService checkpointService;
    private final SourceDataReader reader;
    private final TargetDataWriter writer;
    private final AdaptiveChunkSizer chunkSizer;
    private final LagMonitor lagMonitor;
    private final RetryQueueProcessor retryProcessor;
    private final SyncMetrics metrics;

    public void execute() {
        String jobName = "ORDERS_SYNC";
        long lastId = checkpointService.getLastId(jobName);
        int chunkSize = chunkSizer.currentSize();

        // ① Source 조회
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

        // ② Write
        long writeStart = System.currentTimeMillis();
        try {
            writer.bulkUpsert(records);
        } catch (TransientSyncException e) {
            // Spring Retry가 이미 3회 시도 후 여기에 도달
            retryProcessor.enqueue(records, e);
            return;
        } catch (PermanentSyncException e) {
            log.error("[SyncJob] 영구 오류 — DLQ 이동", e);
            retryProcessor.enqueue(records, e); // or dead(records, e);
            return;
        }
        long writeMs = System.currentTimeMillis() - writeStart;
        log.info("[SyncJob] Write {} 건 / {}ms", records.size(), writeMs);
        metrics.recordWriteTime(writeMs);

        // ③ 동적 청크 사이즈 조절
        chunkSizer.adjust(writeMs);

        // ④ 체크포인트 갱신
        long newLastId = records.get(records.size() - 1).getId();
        checkpointService.update(jobName, newLastId, records.size());

        // ⑤ Lag 모니터링
        lagMonitor.check(jobName, newLastId);

        // ⑥ retry_queue 처리 (이번 tick 남은 시간 활용)
        retryProcessor.processQueue();
    }
}
