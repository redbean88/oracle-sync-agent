package com.example.sync.writer;

import com.example.sync.OracleTestBase;
import com.example.sync.domain.dto.SourceRecordDto;
import com.example.sync.repository.writer.TargetDataWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TargetDataWriterTest extends OracleTestBase {

    @Autowired
    private TargetDataWriter targetDataWriter;

    @Autowired
    @Qualifier("targetJdbcTemplate")
    private JdbcTemplate targetJdbcTemplate;

    @BeforeEach
    void setUp() {
        targetJdbcTemplate.execute("BEGIN EXECUTE IMMEDIATE 'DROP TABLE orders_target'; EXCEPTION WHEN OTHERS THEN NULL; END;");
        targetJdbcTemplate.execute("CREATE TABLE orders_target (id NUMBER PRIMARY KEY, order_no VARCHAR2(50), customer_id NUMBER, status VARCHAR2(20), amount NUMBER(10,2), created_at TIMESTAMP)");
    }

    @Test
    void bulkUpsert_데이터가_성공적으로_병합된다() {
        SourceRecordDto record1 = new SourceRecordDto(1L, "ORD-1", 1001L, "CREATED", new BigDecimal("100.00"), LocalDateTime.now());
        targetDataWriter.bulkUpsert(Arrays.asList(record1));

        Integer count = targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders_target", Integer.class);
        assertThat(count).isEqualTo(1);

        SourceRecordDto updated = new SourceRecordDto(1L, "ORD-1", 1001L, "COMPLETED", new BigDecimal("150.00"), LocalDateTime.now());
        targetDataWriter.bulkUpsert(Arrays.asList(updated));

        String status = targetJdbcTemplate.queryForObject("SELECT status FROM orders_target WHERE id = 1", String.class);
        assertThat(status).isEqualTo("COMPLETED");
    }
}
