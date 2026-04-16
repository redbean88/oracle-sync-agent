package com.example.sync.reader;

import com.example.sync.reader.dto.SourceRecordDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SourceDataReader {

    @Qualifier("sourceJdbcTemplate")
    private final JdbcTemplate sourceJdbcTemplate;

    // readOnly 트랜잭션 분리 — 별도 Service에 위임해야 AOP 프록시 적용됨
    @Transactional(readOnly = true, transactionManager = "sourceTransactionManager")
    public List<SourceRecordDto> fetchChunk(long lastId, int chunkSize) {
        // Oracle 12c+ 방언 사용
        // Oracle INDEX 힌트: 실행계획 이탈 방지
        String sql = "SELECT /*+ INDEX(t IDX_ORDERS_ID) */ " +
                     "       t.id, t.order_no, t.customer_id, t.status, t.amount, t.created_at " +
                     "FROM   orders t " +
                     "WHERE  t.id > ? " +
                     "ORDER BY t.id " +
                     "FETCH FIRST ? ROWS ONLY";
                     
        return sourceJdbcTemplate.query(
            sql,
            (rs, row) -> new SourceRecordDto(
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
