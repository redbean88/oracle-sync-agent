package com.example.sync.service.retry;

import com.example.sync.domain.proxy.BatchRetryQueueEntity;
import com.example.sync.repository.proxy.BatchRetryQueueJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class RetryQueueRepository {

    private final BatchRetryQueueJpaRepository jpaRepository;

    public RetryQueueRepository(BatchRetryQueueJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /** SYSTIMESTAMP + backoffSec으로 next_retry_at 설정. backoffSec=0이면 즉시 처리 가능. */
    @Transactional(transactionManager = "proxyTransactionManager")
    public void save(BatchRetryQueue entry, int backoffSec) {
        jpaRepository.enqueueWithDbTime(
            entry.getSourceIds(),
            entry.getErrorType(),
            entry.getErrorMessage(),
            entry.getMaxRetry(),
            entry.getStatus(),
            backoffSec
        );
    }

    /**
     * FOR UPDATE SKIP LOCKED로 PENDING row들을 claim하고 status를 PROCESSING으로 전이.
     * 별도의 짧은 트랜잭션으로 실행하여 row lock을 빨리 해제.
     */
    @Transactional(transactionManager = "proxyTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public List<BatchRetryQueue> claimPending(int limit) {
        List<BatchRetryQueueEntity> rows = jpaRepository.claimPendingForUpdate(limit);
        for (BatchRetryQueueEntity e : rows) {
            e.setStatus(RetryStatus.PROCESSING);
        }
        jpaRepository.saveAll(rows);
        return rows.stream()
                .map(e -> BatchRetryQueue.builder()
                        .id(e.getId())
                        .sourceIds(e.getSourceIds())
                        .errorType(e.getErrorType())
                        .errorMessage(e.getErrorMessage())
                        .retryCount(e.getRetryCount())
                        .maxRetry(e.getMaxRetry())
                        .nextRetryAt(e.getNextRetryAt())
                        .status(e.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    public void markSuccess(Long id) {
        jpaRepository.markSuccess(id);
    }

    public void markDead(Long id) {
        jpaRepository.markDead(id);
    }

    /** SYSTIMESTAMP 기준으로 backoffSec 후 재시도. */
    public void incrementRetry(Long id, int backoffSec) {
        jpaRepository.incrementRetryWithDbTime(id, backoffSec);
    }

    public long countPending() {
        return jpaRepository.countPending();
    }
}
