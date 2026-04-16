package com.example.sync.retry;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RetryQueueRepository {

    @Qualifier("targetJdbcTemplate")
    private final JdbcTemplate jdbcTemplate;

    public void save(BatchRetryQueue entry) {
        String sql = "INSERT INTO batch_retry_queue (source_ids, error_type, error_message, retry_count, max_retry, next_retry_at, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                entry.getSourceIds(),
                entry.getErrorType(),
                entry.getErrorMessage(),
                entry.getRetryCount(),
                entry.getMaxRetry(),
                Timestamp.valueOf(entry.getNextRetryAt()),
                entry.getStatus()
        );
    }

    public List<BatchRetryQueue> findRetryable(LocalDateTime now) {
        String sql = "SELECT id, source_ids, error_type, error_message, retry_count, max_retry, next_retry_at, status " +
                     "FROM batch_retry_queue " +
                     "WHERE status = 'PENDING' AND next_retry_at <= ?";
        return jdbcTemplate.query(sql,
                (rs, rowNum) -> BatchRetryQueue.builder()
                        .id(rs.getLong("id"))
                        .sourceIds(rs.getString("source_ids"))
                        .errorType(rs.getString("error_type"))
                        .errorMessage(rs.getString("error_message"))
                        .retryCount(rs.getInt("retry_count"))
                        .maxRetry(rs.getInt("max_retry"))
                        .nextRetryAt(rs.getTimestamp("next_retry_at").toLocalDateTime())
                        .status(rs.getString("status"))
                        .build(),
                Timestamp.valueOf(now));
    }

    public void markSuccess(Long id) {
        jdbcTemplate.update("UPDATE batch_retry_queue SET status = 'SUCCESS' WHERE id = ?", id);
    }

    public void markDead(Long id) {
        jdbcTemplate.update("UPDATE batch_retry_queue SET status = 'DEAD' WHERE id = ?", id);
    }

    public void incrementRetry(Long id, LocalDateTime nextRetryAt) {
        jdbcTemplate.update("UPDATE batch_retry_queue SET retry_count = retry_count + 1, next_retry_at = ? WHERE id = ?",
                Timestamp.valueOf(nextRetryAt), id);
    }
}
