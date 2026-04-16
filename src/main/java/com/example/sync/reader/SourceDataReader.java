package com.example.sync.reader;

import com.example.sync.reader.dto.SourceRecordDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

@Service
public class SourceDataReader {

    private final JdbcTemplate sourceJdbcTemplate;

    public SourceDataReader(@Qualifier("sourceJdbcTemplate") JdbcTemplate sourceJdbcTemplate) {
        this.sourceJdbcTemplate = sourceJdbcTemplate;
    }

    @Transactional(readOnly = true, transactionManager = "sourceTransactionManager")
    public List<SourceRecordDto> fetchChunk(long lastId, int chunkSize) {
        String sql = "SELECT /*+ INDEX(t IDX_ORDERS_ID) */ " +
                     "       t.id, t.order_no, t.customer_id, t.status, t.amount, t.created_at " +
                     "FROM   orders t " +
                     "WHERE  t.id > ? " +
                     "ORDER BY t.id " +
                     "FETCH FIRST ? ROWS ONLY";
                     
        return sourceJdbcTemplate.query(
            sql,
            (rs, rowNum) -> new SourceRecordDto(
                rs.getLong("id"),
                rs.getString("order_no"),
                rs.getLong("customer_id"),
                rs.getString("status"),
                rs.getBigDecimal("amount"),
                rs.getTimestamp("created_at").toLocalDateTime()
            ),
            lastId, chunkSize
        );
    }
}
