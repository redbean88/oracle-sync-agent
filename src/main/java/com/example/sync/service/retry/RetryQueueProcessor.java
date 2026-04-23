package com.example.sync.service.retry;

import com.example.sync.config.RetryConfig;
import com.example.sync.service.adapter.SyncJobAdapter;
import com.example.sync.service.monitoring.SyncMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Job별 인스턴스. Spring bean이 아닌 각 스케줄러 @PostConstruct에서 생성. */
public class RetryQueueProcessor<T> {

    private static final Logger log = LoggerFactory.getLogger(RetryQueueProcessor.class);

    private final SyncJobAdapter<T> adapter;
    private final RetryQueueRepository repo;
    private final DeadLetterHandler deadLetterHandler;
    private final SyncMetrics metrics;
    private final RetryConfig retryConfig;

    public RetryQueueProcessor(SyncJobAdapter<T> adapter,
                               RetryQueueRepository repo,
                               DeadLetterHandler deadLetterHandler,
                               SyncMetrics metrics,
                               RetryConfig retryConfig) {
        this.adapter = adapter;
        this.repo = repo;
        this.deadLetterHandler = deadLetterHandler;
        this.metrics = metrics;
        this.retryConfig = retryConfig;
    }

    public void enqueue(List<T> records, Exception e) {
        persistEntry(records, e, retryConfig.getMaxRetry(), RetryStatus.PENDING, retryConfig.getInitialBackoffSec());
        log.warn("[RetryQueue][{}] {} 건 재시도 큐에 등록", adapter.getJobName(), records.size());
    }

    public void enqueueDead(List<T> records, Exception e) {
        persistEntry(records, e, 0, RetryStatus.DEAD, 0);
        log.error("[RetryQueue][{}] {} 건 영구 오류 DEAD 처리", adapter.getJobName(), records.size());
    }

    private void persistEntry(List<T> records, Exception e,
                               int maxRetry, RetryStatus status, int backoffSec) {
        String ids = records.stream()
                .map(r -> String.valueOf(adapter.extractLastId(r)))
                .collect(Collectors.joining(","));

        BatchRetryQueue entry = BatchRetryQueue.builder()
                .sourceIds(ids)
                .errorType(e.getClass().getSimpleName())
                .errorMessage(limitMessage(e.getMessage()))
                .maxRetry(maxRetry)
                .status(status)
                .jobName(adapter.getJobName())
                .build();
        repo.save(entry, backoffSec);
    }

    public void processQueue() {
        List<BatchRetryQueue> claimed = repo.claimPending(adapter.getJobName(), retryConfig.getClaimBatchSize());
        if (claimed.isEmpty()) {
            if (repo.countPending(adapter.getJobName()) > 0) {
                metrics.incrementRetryClaimZero(adapter.getJobName());
                log.warn("[RetryQueue][{}] PENDING 항목이 있으나 CLAIM 0 — 다른 인스턴스가 점유 중", adapter.getJobName());
            }
            return;
        }

        log.info("[RetryQueue][{}] {} 건 claim 완료", adapter.getJobName(), claimed.size());
        claimed.forEach(this::processOneItem);
    }

    public void processOneItem(BatchRetryQueue item) {
        try {
            List<Long> ids = Arrays.stream(item.getSourceIds().split(","))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            List<T> records = adapter.fetchByIds(ids);

            if (records.isEmpty()) {
                log.error("[RetryTask][{}] 원본 데이터 조회 실패 (IDs: {}) - DEAD", adapter.getJobName(), item.getSourceIds());
                deadLetterHandler.handle(item);
                return;
            }

            adapter.bulkUpsert(records);
            repo.markSuccess(item.getId());
            metrics.incrementRetrySuccess(adapter.getJobName());
            log.info("[RetryTask][{}] 복구 완료 (IDs: {})", adapter.getJobName(), item.getSourceIds());
        } catch (Exception e) {
            int currentRetry = item.getRetryCount() + 1;
            if (currentRetry >= item.getMaxRetry()) {
                deadLetterHandler.handle(item);
                log.error("[RetryTask][{}] 최종 재시도 실패 - DEAD (IDs: {})", adapter.getJobName(), item.getSourceIds(), e);
            } else {
                int backoffSec = retryConfig.getBackoffStepSec() * currentRetry;
                repo.incrementRetry(item.getId(), backoffSec);
                log.warn("[RetryTask][{}] 재시도 실패 ({}회), {}초 후 재시도", adapter.getJobName(), currentRetry, backoffSec);
            }
        }
    }

    private String limitMessage(String msg) {
        if (msg == null) return "";
        return msg.length() > 2000 ? msg.substring(0, 2000) : msg;
    }
}
