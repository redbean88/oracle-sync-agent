package com.example.sync.service.retry;

import com.example.sync.domain.proxy.BatchRetryQueue;
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

    @Transactional(transactionManager = "proxyTransactionManager")
    public void save(com.example.sync.service.retry.BatchRetryQueue entry, int backoffSec) {
        jpaRepository.enqueueWithDbTime(
            entry.getSourceIds(),
            entry.getErrorType(),
            entry.getErrorMessage(),
            entry.getMaxRetry(),
            entry.getStatus() != null ? entry.getStatus().name() : RetryStatus.PENDING.name(),
            backoffSec,
            entry.getJobName() != null ? entry.getJobName() : "ORDERS_SYNC"
        );
    }

    /**
     * job_name별로 PENDING 항목을 FOR UPDATE SKIP LOCKED으로 claim하고 PROCESSING으로 전이.
     */
    @Transactional(transactionManager = "proxyTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public List<com.example.sync.service.retry.BatchRetryQueue> claimPending(String jobName, int limit) {
        List<BatchRetryQueue> rows = jpaRepository.claimPendingForUpdate(jobName, limit);
        for (BatchRetryQueue e : rows) {
            e.setStatus(RetryStatus.PROCESSING.name());
        }
        jpaRepository.saveAll(rows);
        return rows.stream()
                .map(e -> com.example.sync.service.retry.BatchRetryQueue.builder()
                        .id(e.getId())
                        .sourceIds(e.getSourceIds())
                        .errorType(e.getErrorType())
                        .errorMessage(e.getErrorMessage())
                        .retryCount(e.getRetryCount())
                        .maxRetry(e.getMaxRetry())
                        .nextRetryAt(e.getNextRetryAt())
                        .status(e.getStatus() != null ? RetryStatus.valueOf(e.getStatus()) : null)
                        .jobName(e.getJobName())
                        .build())
                .collect(Collectors.toList());
    }

    public void markSuccess(Long id) {
        jpaRepository.markSuccess(id);
    }

    public void markDead(Long id) {
        jpaRepository.markDead(id);
    }

    public void incrementRetry(Long id, int backoffSec) {
        jpaRepository.incrementRetryWithDbTime(id, backoffSec);
    }

    public long countPending(String jobName) {
        return jpaRepository.countPending(jobName);
    }

    public int reapStuckProcessing(int staleSec) {
        return jpaRepository.reapStuckProcessing(staleSec);
    }

    public int purgeOldSuccess(int retainSec) {
        return jpaRepository.purgeOldSuccess(retainSec);
    }
}
