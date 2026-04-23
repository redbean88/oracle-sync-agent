package com.example.sync.repository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SyncCheckpointRepository {

    private final JdbcTemplate proxyJdbc;

    public SyncCheckpointRepository(@Qualifier("proxyJdbcTemplate") JdbcTemplate proxyJdbc) {
        this.proxyJdbc = proxyJdbc;
    }

    public long findLastId(String jobName) {
        List<Long> rs = proxyJdbc.query(
            "SELECT last_id FROM sync_checkpoint WHERE job_name = ?",
            (r, i) -> r.getLong(1), jobName);
        return rs.isEmpty() ? 0L : rs.get(0);
    }

    public boolean existsById(String jobName) {
        Integer cnt = proxyJdbc.queryForObject(
            "SELECT COUNT(*) FROM sync_checkpoint WHERE job_name = ?", Integer.class, jobName);
        return cnt != null && cnt > 0;
    }

    public void insert(String jobName, long lastId, long processedCnt, Integer chunkSize) {
        proxyJdbc.update(
            "INSERT INTO sync_checkpoint (job_name, last_id, last_synced_at, processed_cnt, chunk_size) " +
            "VALUES (?, ?, SYSTIMESTAMP, ?, ?)",
            jobName, lastId, processedCnt, chunkSize);
    }

    public int bumpForward(String jobName, long lastId, long delta, Integer chunkSize) {
        return proxyJdbc.update(
            "UPDATE sync_checkpoint SET last_id = ?, last_synced_at = SYSTIMESTAMP, " +
            "processed_cnt = processed_cnt + ?, chunk_size = ? " +
            "WHERE job_name = ? AND last_id < ?",
            lastId, delta, chunkSize, jobName, lastId);
    }
}
