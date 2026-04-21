package com.example.sync.repository.proxy;

import com.example.sync.domain.proxy.BatchRetryQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BatchRetryQueueJpaRepository extends JpaRepository<BatchRetryQueueEntity, Long> {

    /**
     * PENDING 항목을 원자적으로 CLAIM.
     * Oracle 12c+ FOR UPDATE SKIP LOCKED으로 다른 인스턴스가 잠근 row는 건너뜀.
     * SYSTIMESTAMP로 DB 시계 기준 비교 — JVM 시계 편차 무관.
     */
    @Query(value =
        "SELECT * FROM batch_retry_queue " +
        "WHERE id IN (" +
        "  SELECT id FROM batch_retry_queue " +
        "  WHERE status = 'PENDING' AND next_retry_at <= SYSTIMESTAMP " +
        "  ORDER BY next_retry_at ASC " +
        "  FETCH FIRST :limit ROWS ONLY" +
        ") " +
        "FOR UPDATE SKIP LOCKED",
        nativeQuery = true)
    List<BatchRetryQueueEntity> claimPendingForUpdate(@Param("limit") int limit);

    /**
     * SYSTIMESTAMP + backoffSec으로 next_retry_at 설정.
     * GENERATED ALWAYS AS IDENTITY 컬럼이므로 id 생략.
     */
    @Modifying
    @Transactional
    @Query(value =
        "INSERT INTO batch_retry_queue " +
        "  (source_ids, error_type, error_message, retry_count, max_retry, next_retry_at, status) " +
        "VALUES " +
        "  (:sourceIds, :errorType, :errorMessage, 0, :maxRetry, " +
        "   SYSTIMESTAMP + NUMTODSINTERVAL(:backoffSec, 'SECOND'), :status)",
        nativeQuery = true)
    void enqueueWithDbTime(@Param("sourceIds") String sourceIds,
                           @Param("errorType") String errorType,
                           @Param("errorMessage") String errorMessage,
                           @Param("maxRetry") int maxRetry,
                           @Param("status") String status,
                           @Param("backoffSec") int backoffSec);

    /**
     * 재시도 횟수 증가 + SYSTIMESTAMP 기준 next_retry_at 갱신.
     * @param backoffSec 다음 재시도까지 대기 초 (예: 5분 * 재시도횟수 = 300 * n)
     */
    @Modifying
    @Transactional
    @Query(value =
        "UPDATE batch_retry_queue " +
        "SET retry_count = retry_count + 1, " +
        "    next_retry_at = SYSTIMESTAMP + NUMTODSINTERVAL(:backoffSec, 'SECOND'), " +
        "    status = 'PENDING' " +
        "WHERE id = :id",
        nativeQuery = true)
    void incrementRetryWithDbTime(@Param("id") Long id, @Param("backoffSec") int backoffSec);

    @Modifying
    @Transactional
    @Query("UPDATE BatchRetryQueueEntity q SET q.status = 'SUCCESS' WHERE q.id = :id")
    void markSuccess(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE BatchRetryQueueEntity q SET q.status = 'DEAD' WHERE q.id = :id")
    void markDead(@Param("id") Long id);

    @Query("SELECT COUNT(q) FROM BatchRetryQueueEntity q WHERE q.status = 'PENDING'")
    long countPending();
}
