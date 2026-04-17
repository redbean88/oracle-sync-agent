package com.example.sync.infrastructure.retry;

import com.example.sync.domain.proxy.BatchRetryQueueEntity;
import com.example.sync.repository.proxy.BatchRetryQueueJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class RetryQueueRepository {

    private final BatchRetryQueueJpaRepository jpaRepository;

    public RetryQueueRepository(BatchRetryQueueJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public void save(BatchRetryQueue entry) {
        BatchRetryQueueEntity entity = BatchRetryQueueEntity.builder()
            .sourceIds(entry.getSourceIds())
            .errorType(entry.getErrorType())
            .errorMessage(entry.getErrorMessage())
            .retryCount(entry.getRetryCount())
            .maxRetry(entry.getMaxRetry())
            .nextRetryAt(entry.getNextRetryAt())
            .status(entry.getStatus())
            .build();
        jpaRepository.save(entity);
    }

    public List<BatchRetryQueue> findRetryable(LocalDateTime now) {
        return jpaRepository.findRetryable(now).stream()
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

    public void incrementRetry(Long id, LocalDateTime nextRetryAt) {
        jpaRepository.incrementRetry(id, nextRetryAt);
    }
}
