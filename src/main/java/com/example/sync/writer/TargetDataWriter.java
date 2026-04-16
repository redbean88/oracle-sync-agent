package com.example.sync.writer;

import com.example.sync.exception.TransientSyncException;
import com.example.sync.reader.dto.SourceRecordDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TargetDataWriter {

    @Qualifier("targetJdbcTemplate")
    private final JdbcTemplate targetJdbcTemplate;

    @Retryable(
        retryFor  = TransientSyncException.class,
        maxAttempts = 3,
        backoff   = @Backoff(delay = 1000, multiplier = 2)  // 1s → 2s → 4s
    )
    @Transactional(transactionManager = "targetTransactionManager")
    public void bulkUpsert(List<SourceRecordDto> records) {
        String mergeSql = "MERGE INTO orders_target t " +
            "USING (SELECT ? AS id, ? AS order_no, ? AS customer_id, " +
            "              ? AS status, ? AS amount, ? AS created_at " +
            "       FROM dual) s " +
            "ON (t.id = s.id) " +
            "WHEN MATCHED THEN " +
            "    UPDATE SET t.order_no    = s.order_no, " +
            "               t.customer_id = s.customer_id, " +
            "               t.status      = s.status, " +
            "               t.amount      = s.amount, " +
            "               t.created_at  = s.created_at " +
            "WHEN NOT MATCHED THEN " +
            "    INSERT (id, order_no, customer_id, status, amount, created_at) " +
            "    VALUES (s.id, s.order_no, s.customer_id, s.status, s.amount, s.created_at)";

        targetJdbcTemplate.batchUpdate(mergeSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SourceRecordDto r = records.get(i);
                ps.setLong(1, r.getId());
                ps.setString(2, r.getOrderNo());
                ps.setLong(3, r.getCustomerId());
                ps.setString(4, r.getStatus());
                ps.setBigDecimal(5, r.getAmount());
                ps.setTimestamp(6, Timestamp.valueOf(r.getCreatedAt()));
            }
            @Override
            public int getBatchSize() { return records.size(); }
        });
    }

    @Recover
    public void recover(TransientSyncException e, List<SourceRecordDto> records) {
        // 3회 모두 실패 → 호출 측에서 retry_queue로 이동
        throw e;
    }
}
