package com.example.sync.repository.proxy;

import com.example.sync.domain.proxy.SyncCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface SyncCheckpointRepository extends JpaRepository<SyncCheckpoint, String> {

    /**
     * 단조 증가 가드 포함 원자적 업데이트. SYSTIMESTAMP로 DB 시계 기준 기록.
     * split-brain 상황에서도 checkpoint가 뒤로 가지 않도록 {@code last_id < :lastId} 조건을 강제.
     * @return 갱신된 row 수 (0이면 최초 생성 또는 이미 더 큰 lastId가 저장된 상태)
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE sync_checkpoint " +
                   "SET last_id = :lastId, last_synced_at = SYSTIMESTAMP, " +
                   "processed_cnt = processed_cnt + :delta, chunk_size = :chunkSize " +
                   "WHERE job_name = :jobName AND last_id < :lastId",
           nativeQuery = true)
    int bumpForward(@Param("jobName") String jobName,
                    @Param("lastId") long lastId,
                    @Param("delta") long delta,
                    @Param("chunkSize") Integer chunkSize);
}
