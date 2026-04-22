package com.example.sync.repository.proxy;

import com.example.sync.domain.proxy.BatchRetryQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface BatchRetryQueueJpaRepository extends JpaRepository<BatchRetryQueue, Long> {

    /**
     * job_name별로 PENDING 항목을 원자적으로 CLAIM.
     * Oracle 12c+ FOR UPDATE SKIP LOCKED으로 다른 인스턴스가 잠근 row는 건너뜀.
     */
    @Query(value =
        "SELECT * FROM batch_retry_queue " +
        "WHERE id IN (" +
        "  SELECT id FROM batch_retry_queue " +
        "  WHERE status = 'PENDING' AND job_name = :jobName " +
        "    AND next_retry_at <= CAST(SYSTIMESTAMP AS TIMESTAMP) " +
        "  ORDER BY next_retry_at ASC " +
        "  FETCH FIRST :limit ROWS ONLY" +
        ") " +
        "FOR UPDATE SKIP LOCKED",
        nativeQuery = true)
    List<BatchRetryQueue> claimPendingForUpdate(@Param("jobName") String jobName,
                                                @Param("limit") int limit);

    @Modifying
    @Transactional
    @Query(value =
        "INSERT INTO batch_retry_queue " +
        "  (source_ids, error_type, error_message, retry_count, max_retry, next_retry_at, status, job_name) " +
        "VALUES " +
        "  (:sourceIds, :errorType, :errorMessage, 0, :maxRetry, " +
        "   CAST(SYSTIMESTAMP AS TIMESTAMP) + NUMTODSINTERVAL(:backoffSec, 'SECOND'), :status, :jobName)",
        nativeQuery = true)
    void enqueueWithDbTime(@Param("sourceIds") String sourceIds,
                           @Param("errorType") String errorType,
                           @Param("errorMessage") String errorMessage,
                           @Param("maxRetry") int maxRetry,
                           @Param("status") String status,
                           @Param("backoffSec") int backoffSec,
                           @Param("jobName") String jobName);

    @Modifying
    @Transactional
    @Query(value =
        "UPDATE batch_retry_queue " +
        "SET retry_count = retry_count + 1, " +
        "    next_retry_at = CAST(SYSTIMESTAMP AS TIMESTAMP) + NUMTODSINTERVAL(:backoffSec, 'SECOND'), " +
        "    status = 'PENDING' " +
        "WHERE id = :id",
        nativeQuery = true)
    void incrementRetryWithDbTime(@Param("id") Long id, @Param("backoffSec") int backoffSec);

    @Modifying
    @Transactional
    @Query("UPDATE BatchRetryQueue q SET q.status = 'SUCCESS' WHERE q.id = :id")
    void markSuccess(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("UPDATE BatchRetryQueue q SET q.status = 'DEAD' WHERE q.id = :id")
    void markDead(@Param("id") Long id);

    @Query("SELECT COUNT(q) FROM BatchRetryQueue q WHERE q.status = 'PENDING' AND q.jobName = :jobName")
    long countPending(@Param("jobName") String jobName);

    /**
     * 인스턴스 장애로 PROCESSING 상태에 stuck된 항목을 PENDING으로 복구.
     * staleSec 이상 PROCESSING 상태인 항목 대상.
     */
    @Modifying
    @Transactional
    @Query(value =
        "UPDATE batch_retry_queue SET status = 'PENDING' " +
        "WHERE status = 'PROCESSING' " +
        "  AND next_retry_at < CAST(SYSTIMESTAMP AS TIMESTAMP) - NUMTODSINTERVAL(:staleSec, 'SECOND')",
        nativeQuery = true)
    int reapStuckProcessing(@Param("staleSec") int staleSec);

    /**
     * 일정 기간이 지난 SUCCESS 항목 정리 (7일 = 604800초).
     */
    @Modifying
    @Transactional
    @Query(value =
        "DELETE FROM batch_retry_queue WHERE status = 'SUCCESS' " +
        "  AND next_retry_at < CAST(SYSTIMESTAMP AS TIMESTAMP) - NUMTODSINTERVAL(:retainSec, 'SECOND')",
        nativeQuery = true)
    int purgeOldSuccess(@Param("retainSec") int retainSec);
}
