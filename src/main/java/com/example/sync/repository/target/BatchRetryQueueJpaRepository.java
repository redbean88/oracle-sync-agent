package com.example.sync.repository.target;

import com.example.sync.entity.target.BatchRetryQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface BatchRetryQueueJpaRepository extends JpaRepository<BatchRetryQueueEntity, Long> {

    @Query("SELECT q FROM BatchRetryQueueEntity q WHERE q.status = 'PENDING' AND q.nextRetryAt <= :now")
    List<BatchRetryQueueEntity> findRetryable(@Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE BatchRetryQueueEntity q SET q.status = 'SUCCESS' WHERE q.id = :id")
    void markSuccess(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE BatchRetryQueueEntity q SET q.status = 'DEAD' WHERE q.id = :id")
    void markDead(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE BatchRetryQueueEntity q SET q.retryCount = q.retryCount + 1, q.nextRetryAt = :nextRetryAt WHERE q.id = :id")
    void incrementRetry(@Param("id") Long id, @Param("nextRetryAt") LocalDateTime nextRetryAt);
}
