package com.example.sync.service;

import com.example.sync.infrastructure.batch.AdaptiveChunkSizer;
import com.example.sync.infrastructure.exception.PermanentSyncException;
import com.example.sync.infrastructure.exception.TransientSyncException;
import com.example.sync.service.adapter.SyncJobAdapter;
import com.example.sync.service.checkpoint.CheckpointService;
import com.example.sync.service.monitoring.SyncMetrics;
import com.example.sync.service.retry.RetryQueueProcessor;
import javax.annotation.PreDestroy;
import net.javacrumbs.shedlock.core.LockAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SyncJobExecutor {

    private static final Logger log = LoggerFactory.getLogger(SyncJobExecutor.class);
    private static final long MAX_TICK_DURATION_MS = 300_000;

    private final CheckpointService checkpointService;
    private final SyncMetrics metrics;
    private volatile boolean shuttingDown = false;

    public SyncJobExecutor(CheckpointService checkpointService, SyncMetrics metrics) {
        this.checkpointService = checkpointService;
        this.metrics = metrics;
    }

    @PreDestroy
    public void shutdown() {
        log.info("[SyncJob] Shutdown 시그널 수신 - 진행 중인 청크 완료 후 종료");
        this.shuttingDown = true;
    }

    public <T> void execute(SyncJobAdapter<T> adapter,
                            AdaptiveChunkSizer chunkSizer,
                            RetryQueueProcessor<T> retryProcessor,
                            long lagAlertThreshold) {
        LockAssert.assertLocked();

        if (shuttingDown) {
            log.warn("[{}] Shutdown 진행 중이므로 새로운 작업을 시작하지 않습니다.", adapter.getJobName());
            return;
        }

        String jobName = adapter.getJobName();
        int totalProcessedCount = 0;
        long tickStartTime = System.currentTimeMillis();

        log.info("[{}] 동기화 작업 시작 (연속 청크 처리 모드)", jobName);

        try {
            while (!shuttingDown) {
                long lastId = checkpointService.getLastId(jobName);
                int chunkSize = chunkSizer.getChunkSize();

                long readStart = System.currentTimeMillis();
                List<T> records;
                try {
                    records = adapter.readChunk(lastId, chunkSize);
                } catch (Exception e) {
                    log.error("[{}] 데이터 조회 중 오류 발생 (lastId: {})", jobName, lastId, e);
                    metrics.incrementReadError(jobName);
                    break;
                }
                long readDuration = System.currentTimeMillis() - readStart;

                if (records.isEmpty()) {
                    log.info("[{}] 현재 처리할 백로그가 없습니다. (총 {} 건 처리 완료)", jobName, totalProcessedCount);
                    break;
                }

                long writeStart = System.currentTimeMillis();
                try {
                    adapter.bulkUpsert(records);
                } catch (PermanentSyncException e) {
                    log.error("[{}] 영구 오류 발생 - 즉시 DLQ 이동", jobName, e);
                    retryProcessor.enqueueDead(records, e);
                    break;
                } catch (TransientSyncException e) {
                    log.warn("[{}] 일시적 오류 발생 - 재시도 큐로 이동", jobName, e);
                    retryProcessor.enqueue(records, e);
                    break;
                } catch (Exception e) {
                    log.error("[{}] 분류 불가 오류 - 재시도 큐로 이동 (보수적)", jobName, e);
                    retryProcessor.enqueue(records, e);
                    break;
                }
                long writeDuration = System.currentTimeMillis() - writeStart;

                metrics.recordReadTime(jobName, readDuration);
                metrics.recordWriteTime(jobName, writeDuration);
                chunkSizer.adjust(writeDuration);

                long newLastId = adapter.extractLastId(records.get(records.size() - 1));
                checkpointService.update(jobName, newLastId, records.size(), chunkSizer.getChunkSize());
                totalProcessedCount += records.size();

                log.info("[{}] 청크 처리 완료: {} 건 (누적: {}) / LastID: {}", jobName, records.size(), totalProcessedCount, newLastId);

                adapter.checkLag(jobName, newLastId, lagAlertThreshold);

                if (System.currentTimeMillis() - tickStartTime > MAX_TICK_DURATION_MS) {
                    log.warn("[{}] 단일 실행 제한 시간({}) 초과. 다음 스케줄에서 재개합니다.", jobName, MAX_TICK_DURATION_MS);
                    break;
                }

                if (records.size() < chunkSize) {
                    log.info("[{}] 모든 백로그 처리 완료 (총 {} 건)", jobName, totalProcessedCount);
                    break;
                }
            }

            retryProcessor.processQueue();

        } finally {
            long tickDuration = System.currentTimeMillis() - tickStartTime;
            metrics.recordTickDuration(jobName, tickDuration);
            log.info("[{}] 이번 스케줄 Tick 종료. 최종 소요시간: {}ms", jobName, tickDuration);
        }
    }
}
