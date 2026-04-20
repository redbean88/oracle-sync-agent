package com.example.sync.repository.proxy;

import com.example.sync.domain.proxy.BatchRetryQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BatchRetryQueueJpaRepository extends JpaRepository<BatchRetryQueueEntity, Long> {

    /**
     * 재시도 대상을 원자적으로 CLAIM.
     * Oracle 12c+ FOR UPDATE SKIP LOCKED 를 사용하여 다른 인스턴스가 잠근 row는 건너뜀.
     * 호출자는 트랜잭션 내에서 반환된 엔티티의 status를 PROCESSING으로 전이 후 commit해야 함.
     */
    @Query(value =
        "SELECT * FROM batch_retry_queue " +
        "WHERE id IN (" +
        "  SELECT id FROM batch_retry_queue " +
        "  WHERE status = 'PENDING' AND next_retry_at <= :now " +
        "  ORDER BY next_retry_at ASC " +
        "  FETCH FIRST :limit ROWS ONLY" +
        ") " +
        "FOR UPDATE SKIP LOCKED",
        nativeQuery = true)
    List<BatchRetryQueueEntity> claimPendingForUpdate(@Param("now") LocalDateTime now,
                                                      @Param("limit") int limit);

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
    @Query("UPDATE BatchRetryQueueEntity q SET q.retryCount = q.retryCount + 1, q.nextRetryAt = :nextRetryAt, q.status = 'PENDING' WHERE q.id = :id")
    void incrementRetry(@Param("id") Long id, @Param("nextRetryAt") LocalDateTime nextRetryAt);
}
