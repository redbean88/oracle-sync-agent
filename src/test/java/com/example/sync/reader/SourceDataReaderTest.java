package com.example.sync.reader;

import com.example.sync.OracleTestBase;
import com.example.sync.reader.dto.SourceRecordDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SourceDataReaderTest extends OracleTestBase {

    @Autowired
    private SourceDataReader sourceDataReader;

    @Autowired
    @Qualifier("sourceJdbcTemplate")
    private JdbcTemplate sourceJdbcTemplate;

    @BeforeEach
    void setUp() {
        sourceJdbcTemplate.execute("BEGIN EXECUTE IMMEDIATE 'DROP TABLE orders'; EXCEPTION WHEN OTHERS THEN NULL; END;");
        sourceJdbcTemplate.execute("CREATE TABLE orders (id NUMBER PRIMARY KEY, order_no VARCHAR2(50), customer_id NUMBER, status VARCHAR2(20), amount NUMBER(10,2), created_at TIMESTAMP)");
        
        for (long i = 1; i <= 5; i++) {
            sourceJdbcTemplate.update(
                "INSERT INTO orders (id, order_no, customer_id, status, amount, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                i, "ORD-" + i, 1000 + i, "CREATED", new BigDecimal("100.00"), Timestamp.valueOf(LocalDateTime.now())
            );
        }
    }

    @Test
    void fetchChunk_데이터를_크기만큼_가져온다() {
        List<SourceRecordDto> result = sourceDataReader.fetchChunk(0L, 3);
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }
}
