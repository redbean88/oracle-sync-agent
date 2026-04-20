package com.example.sync.repository.proxy;

import com.example.sync.domain.proxy.SyncCheckpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SyncCheckpointRepository extends JpaRepository<SyncCheckpoint, String> {

    /**
     * 단조 증가 가드 포함 원자적 업데이트.
     * split-brain 상황에서도 checkpoint가 뒤로 가지 않도록 {@code c.lastId < :lastId} 조건을 강제.
     * @return 갱신된 row 수 (0이면 최초 생성 또는 이미 더 큰 lastId가 저장된 상태)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE SyncCheckpoint c SET c.lastId = :lastId, " +
           "c.lastSyncedAt = :now, c.processedCnt = c.processedCnt + :delta, " +
           "c.chunkSize = :chunkSize " +
           "WHERE c.jobName = :jobName AND c.lastId < :lastId")
    int bumpForward(@Param("jobName") String jobName,
                    @Param("lastId") long lastId,
                    @Param("now") LocalDateTime now,
                    @Param("delta") long delta,
                    @Param("chunkSize") Integer chunkSize);
}
