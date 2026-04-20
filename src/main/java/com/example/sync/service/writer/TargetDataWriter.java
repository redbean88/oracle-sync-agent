package com.example.sync.service.writer;

import com.example.sync.domain.source.dto.SourceRecordDto;
import com.example.sync.infrastructure.exception.OracleExceptionClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Service
public class TargetDataWriter {

    private static final Logger log = LoggerFactory.getLogger(TargetDataWriter.class);

    private static final String MERGE_SQL =
            "MERGE INTO orders_target t " +
            "USING (SELECT ? AS id, ? AS order_no, ? AS customer_id, " +
            "              ? AS status, ? AS amount, ? AS created_at FROM dual) s " +
            "ON (t.id = s.id) " +
            "WHEN MATCHED THEN UPDATE SET " +
            "     t.status = s.status, t.amount = s.amount, t.order_no = s.order_no " +
            "WHEN NOT MATCHED THEN INSERT " +
            "     (id, order_no, customer_id, status, amount, created_at) " +
            "     VALUES (s.id, s.order_no, s.customer_id, s.status, s.amount, s.created_at)";

    private final JdbcTemplate targetJdbcTemplate;

    public TargetDataWriter(@Qualifier("targetJdbcTemplate") JdbcTemplate targetJdbcTemplate) {
        this.targetJdbcTemplate = targetJdbcTemplate;
    }

    @Transactional(transactionManager = "targetTransactionManager")
    public void bulkUpsert(List<SourceRecordDto> records) {
        if (records.isEmpty()) return;

        try {
        int[] result = targetJdbcTemplate.batchUpdate(MERGE_SQL, new BatchPreparedStatementSetter() {
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
                public int getBatchSize() {
                    return records.size();
                }
            });
            log.info("[MERGE] {} 건 upsert 완료", result.length);
        } catch (DataAccessException dae) {
            // Oracle error-code 기반 Transient/Permanent 분류 후 재throw
            throw OracleExceptionClassifier.classify(dae);
        }
    }
}
