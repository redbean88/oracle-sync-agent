package com.example.sync.checkpoint;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckpointService {

    private final JdbcTemplate jdbcTemplate;

    public CheckpointService(@Qualifier("targetJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(transactionManager = "targetTransactionManager", readOnly = true)
    public long getLastId(String jobName) {
        String sql = "SELECT last_id FROM sync_checkpoint WHERE job_name = ?";
        try {
            Long lastId = jdbcTemplate.queryForObject(sql, Long.class, jobName);
            return lastId != null ? lastId : 0L;
        } catch (EmptyResultDataAccessException e) {
            return 0L;
        }
    }

    @Transactional(transactionManager = "targetTransactionManager")
    public void update(String jobName, long lastId, int processedCnt) {
        String countSql = "SELECT COUNT(*) FROM sync_checkpoint WHERE job_name = ?";
        Integer count = jdbcTemplate.queryForObject(countSql, Integer.class, jobName);
        
        if (count != null && count > 0) {
            String updateSql = "UPDATE sync_checkpoint SET last_id = ?, last_synced_at = CURRENT_TIMESTAMP, processed_cnt = processed_cnt + ? WHERE job_name = ?";
            jdbcTemplate.update(updateSql, lastId, processedCnt, jobName);
        } else {
            String insertSql = "INSERT INTO sync_checkpoint (job_name, last_id, last_synced_at, processed_cnt) VALUES (?, ?, CURRENT_TIMESTAMP, ?)";
            jdbcTemplate.update(insertSql, jobName, lastId, processedCnt);
        }
    }
}
