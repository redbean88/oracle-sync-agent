package com.example.sync.service.adapter;

import com.example.sync.dto.OrdersRecordDto;
import com.example.sync.infrastructure.exception.OracleExceptionClassifier;
import com.example.sync.service.monitoring.LagMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

@Component
public class OrdersSyncAdapter implements SyncJobAdapter<OrdersRecordDto> {

    private static final Logger log = LoggerFactory.getLogger(OrdersSyncAdapter.class);

    private static final String SELECT_CHUNK_SQL =
            "SELECT id, order_no, customer_id, status, amount, created_at " +
            "FROM orders WHERE id > ? ORDER BY id ASC FETCH FIRST ? ROWS ONLY";

    private static final String SELECT_BY_IDS_SQL_TEMPLATE =
            "SELECT id, order_no, customer_id, status, amount, created_at " +
            "FROM orders WHERE id IN (%s)";

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

    private static final RowMapper<OrdersRecordDto> ORDERS_ROW_MAPPER = (rs, i) ->
            new OrdersRecordDto(
                rs.getLong("id"),
                rs.getString("order_no"),
                rs.getLong("customer_id"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                rs.getTimestamp("created_at").toLocalDateTime()
            );

    private final JdbcTemplate sourceJdbc;
    private final JdbcTemplate targetJdbc;
    private final LagMonitor lagMonitor;

    public OrdersSyncAdapter(@Qualifier("sourceJdbcTemplate") JdbcTemplate sourceJdbc,
                             @Qualifier("targetJdbcTemplate") JdbcTemplate targetJdbc,
                             LagMonitor lagMonitor) {
        this.sourceJdbc = sourceJdbc;
        this.targetJdbc = targetJdbc;
        this.lagMonitor = lagMonitor;
    }

    @Override
    public String getJobName() { return "ORDERS_SYNC"; }

    @Override
    public List<OrdersRecordDto> readChunk(long lastId, int chunkSize) {
        return sourceJdbc.query(SELECT_CHUNK_SQL, ORDERS_ROW_MAPPER, lastId, chunkSize);
    }

    @Override
    public List<OrdersRecordDto> fetchByIds(List<Long> ids) {
        if (ids.isEmpty()) return Collections.emptyList();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql = String.format(SELECT_BY_IDS_SQL_TEMPLATE, placeholders);
        return sourceJdbc.query(sql, ORDERS_ROW_MAPPER, ids.toArray());
    }

    @Override
    @Transactional(transactionManager = "targetTransactionManager")
    public void bulkUpsert(List<OrdersRecordDto> records) {
        if (records.isEmpty()) return;
        try {
            int[] result = targetJdbc.batchUpdate(MERGE_SQL, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    OrdersRecordDto r = records.get(i);
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
            log.info("[MERGE] {} 건 upsert 완료", result.length);
        } catch (DataAccessException dae) {
            throw OracleExceptionClassifier.classify(dae);
        }
    }

    @Override
    public long extractLastId(OrdersRecordDto record) { return record.getId(); }

    @Override
    public void checkLag(String jobName, long checkpointId, long threshold) {
        Long maxId = sourceJdbc.queryForObject("SELECT MAX(id) FROM orders", Long.class);
        if (maxId == null) return;
        lagMonitor.recordAndAlert(jobName, maxId - checkpointId, threshold);
    }
}
