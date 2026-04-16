package com.example.sync.reader;

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
class SourceDataReaderTest {

    @Autowired
    private SourceDataReader sourceDataReader;

    @Autowired
    @Qualifier("sourceJdbcTemplate")
    private JdbcTemplate sourceJdbcTemplate;

    @BeforeEach
    void setUp() {
        sourceJdbcTemplate.execute("CREATE TABLE IF NOT EXISTS orders (id BIGINT PRIMARY KEY, order_no VARCHAR(50), customer_id BIGINT, status VARCHAR(20), amount DECIMAL(10,2), created_at TIMESTAMP)");
        sourceJdbcTemplate.execute("DELETE FROM orders");
        
        // 데이터 더미 5건 삽입
        for (long i = 1; i <= 5; i++) {
            sourceJdbcTemplate.update(
                "INSERT INTO orders (id, order_no, customer_id, status, amount, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                i, "ORD-" + i, 1000 + i, "CREATED", new BigDecimal("100.00"), Timestamp.valueOf(LocalDateTime.now())
            );
        }
    }

    @Test
    void fetchChunk_데이터를_크기만큼_가져온다() {
        // given
        long lastId = 0L;
        int chunkSize = 3;

        // when
        List<SourceRecordDto> result = sourceDataReader.fetchChunk(lastId, chunkSize);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(2).getId()).isEqualTo(3L);
    }
}
