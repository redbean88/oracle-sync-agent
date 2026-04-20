package com.example.sync.service.retry;

import com.example.sync.domain.source.dto.SourceRecordDto;
import com.example.sync.service.reader.SourceDataReader;
import com.example.sync.service.writer.TargetDataWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RetryQueueProcessor {

    private static final Logger log = LoggerFactory.getLogger(RetryQueueProcessor.class);
    private static final int CLAIM_BATCH_SIZE = 50;

    private final RetryQueueRepository repo;
    private final SourceDataReader sourceDataReader;
    private final TargetDataWriter targetDataWriter;
    private final DeadLetterHandler deadLetterHandler;

    public RetryQueueProcessor(RetryQueueRepository repo,
                               SourceDataReader sourceDataReader,
                               TargetDataWriter targetDataWriter,
                               DeadLetterHandler deadLetterHandler) {
        this.repo = repo;
        this.sourceDataReader = sourceDataReader;
        this.targetDataWriter = targetDataWriter;
        this.deadLetterHandler = deadLetterHandler;
    }

    /** 실패 시 ID 리스트를 재시도 대상으로 Proxy DB에 기록 (PENDING) */
    public void enqueue(List<SourceRecordDto> records, Exception e) {
        persistQueueEntry(records, e, 3, RetryStatus.PENDING, LocalDateTime.now().plusMinutes(1));
        log.warn("[RetryQueue] {} 건의 데이터 ID를 재시도 큐에 등록", records.size());
    }

    /** Permanent 오류 — 재시도하지 않고 즉시 DEAD */
    public void enqueueDead(List<SourceRecordDto> records, Exception e) {
        persistQueueEntry(records, e, 0, RetryStatus.DEAD, LocalDateTime.now());
        log.error("[RetryQueue] {} 건의 데이터가 영구 오류로 DEAD 처리", records.size());
    }

    private void persistQueueEntry(List<SourceRecordDto> records, Exception e,
                                    int maxRetry, String status, LocalDateTime nextRetryAt) {
        String ids = records.stream()
                .map(r -> String.valueOf(r.getId()))
                .collect(Collectors.joining(","));

        BatchRetryQueue entry = BatchRetryQueue.builder()
                .sourceIds(ids)
                .errorType(e.getClass().getSimpleName())
                .errorMessage(limitMessage(e.getMessage()))
                .retryCount(0)
                .maxRetry(maxRetry)
                .nextRetryAt(nextRetryAt)
                .status(status)
                .build();
        repo.save(entry);
    }

    /** 스케줄에 의해 호출. CLAIM → item 단위 독립 트랜잭션 처리. */
    public void processQueue() {
        List<BatchRetryQueue> claimed = repo.claimPending(LocalDateTime.now(), CLAIM_BATCH_SIZE);
        if (claimed.isEmpty()) return;

        log.info("[RetryQueue] {} 건 claim 완료", claimed.size());
        for (BatchRetryQueue item : claimed) {
            processOneItem(item);
        }
    }

    /** item 단위 독립 트랜잭션 — 일부 실패가 다른 item에 영향을 주지 않도록 격리. */
    @Transactional(transactionManager = "proxyTransactionManager", propagation = Propagation.REQUIRES_NEW)
    void processOneItem(BatchRetryQueue item) {
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
                LocalDateTime next = LocalDateTime.now().plusMinutes(5L * currentRetry);
                repo.incrementRetry(item.getId(), next);
                log.warn("[RetryTask] 재시도 실패 ({}회), 다음 실행: {}", currentRetry, next);
            }
        }
    }

    private String limitMessage(String msg) {
        if (msg == null) return "";
        return msg.length() > 2000 ? msg.substring(0, 2000) : msg;
    }
}
