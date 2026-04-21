package com.example.sync.service.retry;

import com.example.sync.domain.source.dto.SourceRecordDto;
import com.example.sync.service.monitoring.SyncMetrics;
import com.example.sync.service.reader.SourceDataReader;
import com.example.sync.service.writer.TargetDataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RetryQueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(RetryQueueProcessor.class);
    private static final int CLAIM_BATCH_SIZE = 50;
    private static final int INITIAL_BACKOFF_SEC = 60;       // 1분
    private static final int BACKOFF_STEP_SEC    = 5 * 60;   // 5분 단위 지수 백오프

    private final RetryQueueRepository repo;
    private final SourceDataReader sourceDataReader;
    private final TargetDataWriter targetDataWriter;
    private final DeadLetterHandler deadLetterHandler;
    private final SyncMetrics metrics;

    public RetryQueueProcessor(RetryQueueRepository repo,
                               SourceDataReader sourceDataReader,
                               TargetDataWriter targetDataWriter,
                               DeadLetterHandler deadLetterHandler,
                               SyncMetrics metrics) {
        this.repo = repo;
        this.sourceDataReader = sourceDataReader;
        this.targetDataWriter = targetDataWriter;
        this.deadLetterHandler = deadLetterHandler;
        this.metrics = metrics;
    }

    /** 실패 시 ID 리스트를 재시도 대상으로 Proxy DB에 기록 (PENDING, 1분 후) */
    public void enqueue(List<SourceRecordDto> records, Exception e) {
        persistQueueEntry(records, e, 3, RetryStatus.PENDING, INITIAL_BACKOFF_SEC);
        log.warn("[RetryQueue] {} 건의 데이터 ID를 재시도 큐에 등록", records.size());
    }

    /** Permanent 오류 — 재시도하지 않고 즉시 DEAD */
    public void enqueueDead(List<SourceRecordDto> records, Exception e) {
        persistQueueEntry(records, e, 0, RetryStatus.DEAD, 0);
        log.error("[RetryQueue] {} 건의 데이터가 영구 오류로 DEAD 처리", records.size());
    }

    private void persistQueueEntry(List<SourceRecordDto> records, Exception e,
                                    int maxRetry, RetryStatus status, int backoffSec) {
        String ids = records.stream()
                .map(r -> String.valueOf(r.getId()))
                .collect(Collectors.joining(","));

        BatchRetryQueue entry = BatchRetryQueue.builder()
                .sourceIds(ids)
                .errorType(e.getClass().getSimpleName())
                .errorMessage(limitMessage(e.getMessage()))
                .maxRetry(maxRetry)
                .status(status)
                .build();
        repo.save(entry, backoffSec);
    }

    /** 스케줄에 의해 호출. CLAIM → item 단위 독립 트랜잭션 처리. */
    public void processQueue() {
        List<BatchRetryQueue> claimed = repo.claimPending(CLAIM_BATCH_SIZE);
        if (claimed.isEmpty()) {
            if (repo.countPending() > 0) {
                metrics.incrementRetryClaimZero();
                log.warn("[RetryQueue] PENDING 항목이 있으나 CLAIM 0 — 다른 인스턴스가 모두 점유 중");
            }
            return;
        }

        log.info("[RetryQueue] {} 건 claim 완료", claimed.size());
        claimed.forEach(this::processOneItem);
    }

    /** item 단위 독립 트랜잭션 — 일부 실패가 다른 item에 영향을 주지 않도록 격리. */
    @Transactional(transactionManager = "proxyTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void processOneItem(BatchRetryQueue item) {
        try {
            List<Long> ids = Arrays.stream(item.getSourceIds().split(","))
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            List<SourceRecordDto> records = sourceDataReader.fetchByIds(ids);

            if (records.isEmpty()) {
                log.error("[RetryTask] 원본 데이터 조회 실패 (IDs: {}) - FAILED", item.getSourceIds());
                repo.markDead(item.getId());
                deadLetterHandler.handle(item);
                return;
            }

            targetDataWriter.bulkUpsert(records);
            repo.markSuccess(item.getId());
            log.info("[RetryTask] 복구 완료 (IDs: {})", item.getSourceIds());
        } catch (Exception e) {
            int currentRetry = item.getRetryCount() + 1;
            if (currentRetry >= item.getMaxRetry()) {
                repo.markDead(item.getId());
                deadLetterHandler.handle(item);
                log.error("[RetryTask] 최종 재시도 실패 - DEAD 이동 (IDs: {})", item.getSourceIds(), e);
            } else {
                int backoffSec = BACKOFF_STEP_SEC * currentRetry;
                repo.incrementRetry(item.getId(), backoffSec);
                log.warn("[RetryTask] 재시도 실패 ({}회), {}초 후 재시도", currentRetry, backoffSec);
            }
        }
    }

    private String limitMessage(String msg) {
        if (msg == null) return "";
        return msg.length() > 2000 ? msg.substring(0, 2000) : msg;
    }
}
