package com.example.sync.base;

import com.example.sync.infrastructure.batch.AdaptiveChunkSizer;
import com.example.sync.infrastructure.checkpoint.CheckpointService;
import com.example.sync.infrastructure.monitoring.LagMonitor;
import com.example.sync.infrastructure.monitoring.SyncMetrics;
import com.example.sync.repository.reader.SourceDataReader;
import com.example.sync.dto.SourceRecordDto;
import com.example.sync.infrastructure.retry.RetryQueueProcessor;
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

    // 단일 스케줄 실행(Tick) 시 최대 실행 시간 (예: 5분)
    private static final long MAX_TICK_DURATION_MS = 300_000;

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
        log.info("[SyncJob] Shutdown 시그널 수신 - 진행 중인 청크 완료 후 종료");
        this.shuttingDown = true;
    }

    /**
     * 스케줄 실행 시 호출되는 메인 실행 로직
     * 조회된 전체 건수를 chunkSize 단위로 세분화하여 루프를 돌며 처리합니다.
     */
    public void execute() {
        if (shuttingDown) {
            log.warn("[SyncJob] Shutdown 진행 중이므로 새로운 작업을 시작하지 않습니다.");
            return;
        }

        String jobName = "ORDERS_SYNC";
        int totalProcessedCount = 0;
        long tickStartTime = System.currentTimeMillis();

        log.info("[SyncJob] 동기화 작업 시작 (연속 청크 처리 모드)");

        // 더 이상 처리할 데이터가 없거나, 제한 시간에 도달할 때까지 반복
        while (!shuttingDown) {
            long lastId = checkpointService.getLastId(jobName);
            int chunkSize = chunkSizer.currentSize();

            // 1. 세부 단위(Chunk) 데이터 페치
            long readStart = System.currentTimeMillis();
            List<SourceRecordDto> records;
            try {
                records = reader.fetchChunk(lastId, chunkSize);
            } catch (Exception e) {
                log.error("[SyncJob] 데이터 조회 중 오류 발생 (lastId: {})", lastId, e);
                metrics.incrementReadError();
                break;
            }
            long readDuration = System.currentTimeMillis() - readStart;

            if (records.isEmpty()) {
                log.info("[SyncJob] 현재 처리할 백로그가 없습니다. (총 {} 건 처리 완료)", totalProcessedCount);
                break;
            }

            // 2. 세부 단위(Chunk) 데이터 적재
            long writeStart = System.currentTimeMillis();
            try {
                writer.bulkUpsert(records);
            } catch (Exception e) {
                log.error("[SyncJob] 데이터 적재 중 오류 발생 - 재시도 큐로 이동", e);
                retryProcessor.enqueue(records, e);
                break; // 오류 시 정합성을 위해 루프 중단
            }
            long writeDuration = System.currentTimeMillis() - writeStart;

            // 3. 지표 기록 및 체크포인트 갱신 (청크 단위 즉시 반영)
            metrics.recordReadTime(readDuration);
            metrics.recordWriteTime(writeDuration);
            chunkSizer.adjust(writeDuration);

            long newLastId = records.get(records.size() - 1).getId();
            checkpointService.update(jobName, newLastId, records.size());
            totalProcessedCount += records.size();

            log.info("[SyncJob] 청크 처리 완료: {} 건 (누적: {}) / LastID: {}", 
                    records.size(), totalProcessedCount, newLastId);

            // 4. 지연 모니터링
            lagMonitor.check(jobName, newLastId);

            // 5. 실행 시간 제한 체크 (너무 긴 작업 방지)
            if (System.currentTimeMillis() - tickStartTime > MAX_TICK_DURATION_MS) {
                log.warn("[SyncJob] 단일 실행 제한 시간({}) 초과. 다음 스케줄에서 재개합니다.", MAX_TICK_DURATION_MS);
                break;
            }

            // 데이터가 chunkSize보다 적게 왔다면 현재 백로그가 다 비워진 것으로 판단
            if (records.size() < chunkSize) {
                log.info("[SyncJob] 모든 백로그 처리 완료 (총 {} 건)", totalProcessedCount);
                break;
            }
        }

        // 마지막으로 재시도 큐 처리
        retryProcessor.processQueue();
        
        log.info("[SyncJob] 이번 스케줄 Tick 종료. 최종 소요시간: {}ms", System.currentTimeMillis() - tickStartTime);
    }
}
