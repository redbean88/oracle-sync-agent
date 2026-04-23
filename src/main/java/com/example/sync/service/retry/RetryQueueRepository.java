package com.example.sync.service.retry;

import com.example.sync.repository.BatchRetryQueueJdbcRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class RetryQueueRepository {

    private final BatchRetryQueueJdbcRepository jdbcRepo;

    public RetryQueueRepository(BatchRetryQueueJdbcRepository jdbcRepo) {
        this.jdbcRepo = jdbcRepo;
    }

    @Transactional(transactionManager = "proxyTransactionManager")
    public void save(BatchRetryQueue entry, int backoffSec) {
        jdbcRepo.enqueueWithDbTime(
            entry.getSourceIds(),
            entry.getErrorType(),
            entry.getErrorMessage(),
            entry.getMaxRetry(),
            entry.getStatus() != null ? entry.getStatus().name() : RetryStatus.PENDING.name(),
            backoffSec,
            entry.getJobName() != null ? entry.getJobName() : "ORDERS_SYNC"
        );
    }

    @Transactional(transactionManager = "proxyTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public List<BatchRetryQueue> claimPending(String jobName, int limit) {
        List<BatchRetryQueue> rows = jdbcRepo.claimPendingForUpdate(jobName, limit);
        if (rows.isEmpty()) return rows;
        List<Long> ids = rows.stream().map(BatchRetryQueue::getId).collect(Collectors.toList());
        jdbcRepo.markProcessing(ids);
        rows.forEach(r -> r.setStatus(RetryStatus.PROCESSING));
        return rows;
    }

    public void markSuccess(Long id) {
        jdbcRepo.markSuccess(id);
    }

    public void markDead(Long id) {
        jdbcRepo.markDead(id);
    }

    public void incrementRetry(Long id, int backoffSec) {
        jdbcRepo.incrementRetryWithDbTime(id, backoffSec);
    }

    public long countPending(String jobName) {
        return jdbcRepo.countPending(jobName);
    }

    public int reapStuckProcessing(int staleSec) {
        return jdbcRepo.reapStuckProcessing(staleSec);
    }

    public int purgeOldSuccess(int retainSec) {
        return jdbcRepo.purgeOldSuccess(retainSec);
    }
}
