package com.example.sync.writer;

import com.example.sync.reader.dto.SourceRecordDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class TargetDataWriterTest {

    @Autowired
    private TargetDataWriter targetDataWriter;

    @Autowired
    @Qualifier("targetJdbcTemplate")
    private JdbcTemplate targetJdbcTemplate;

    @BeforeEach
    void setUp() {
        targetJdbcTemplate.execute("CREATE TABLE IF NOT EXISTS orders_target (id BIGINT PRIMARY KEY, order_no VARCHAR(50), customer_id BIGINT, status VARCHAR(20), amount DECIMAL(10,2), created_at TIMESTAMP)");
        targetJdbcTemplate.execute("DELETE FROM orders_target");
    }

    @Test
    void bulkUpsert_데이터가_성공적으로_병합된다() {
        // given
        SourceRecordDto record1 = new SourceRecordDto(1L, "ORD-1", 1001L, "CREATED", new BigDecimal("100.00"), LocalDateTime.now());
        SourceRecordDto record2 = new SourceRecordDto(2L, "ORD-2", 1002L, "CREATED", new BigDecimal("200.00"), LocalDateTime.now());
        
        List<SourceRecordDto> chunk = List.of(record1, record2);

        // when
        targetDataWriter.bulkUpsert(chunk);

        // then
        Integer count = targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders_target", Integer.class);
        assertThat(count).isEqualTo(2);
        
        String status = targetJdbcTemplate.queryForObject("SELECT status FROM orders_target WHERE id = 1", String.class);
        assertThat(status).isEqualTo("CREATED");

        // when (UPSERT - Update case)
        SourceRecordDto updatedRecord1 = new SourceRecordDto(1L, "ORD-1", 1001L, "COMPLETED", new BigDecimal("150.00"), LocalDateTime.now());
        targetDataWriter.bulkUpsert(List.of(updatedRecord1));

        // then
        Integer countAfterUpsert = targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders_target", Integer.class);
        assertThat(countAfterUpsert).isEqualTo(2); // Count should remain 2

        String updatedStatus = targetJdbcTemplate.queryForObject("SELECT status FROM orders_target WHERE id = 1", String.class);
        assertThat(updatedStatus).isEqualTo("COMPLETED");

        BigDecimal updatedAmount = targetJdbcTemplate.queryForObject("SELECT amount FROM orders_target WHERE id = 1", BigDecimal.class);
        assertThat(updatedAmount).isEqualByComparingTo(new BigDecimal("150.00"));
    }
}
