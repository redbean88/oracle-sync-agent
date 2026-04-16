package com.example.sync;

import com.example.sync.batch.SyncJobExecutor;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class SyncJobExecutorTest {

    @Autowired
    private SyncJobExecutor syncJobExecutor;

    @Autowired
    @Qualifier("sourceJdbcTemplate")
    private JdbcTemplate sourceJdbcTemplate;

    @Autowired
    @Qualifier("targetJdbcTemplate")
    private JdbcTemplate targetJdbcTemplate;

    @BeforeEach
    void setUp() {
        // Initialize tables for H2
        sourceJdbcTemplate.execute("CREATE TABLE IF NOT EXISTS orders (id BIGINT PRIMARY KEY, order_no VARCHAR(50), customer_id BIGINT, status VARCHAR(20), amount DECIMAL(10,2), created_at TIMESTAMP)");
        targetJdbcTemplate.execute("CREATE TABLE IF NOT EXISTS orders_target (id BIGINT PRIMARY KEY, order_no VARCHAR(50), customer_id BIGINT, status VARCHAR(20), amount DECIMAL(10,2), created_at TIMESTAMP)");
        targetJdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sync_checkpoint (job_name VARCHAR(100) PRIMARY KEY, last_id BIGINT DEFAULT 0, last_synced_at TIMESTAMP, processed_cnt BIGINT DEFAULT 0)");
        targetJdbcTemplate.execute("CREATE TABLE IF NOT EXISTS batch_retry_queue (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, source_ids CLOB, error_type VARCHAR(100), error_message CLOB, retry_count INT DEFAULT 0, max_retry INT DEFAULT 5, next_retry_at TIMESTAMP, status VARCHAR(20) DEFAULT 'PENDING')");
        targetJdbcTemplate.execute("CREATE TABLE IF NOT EXISTS shedlock (name VARCHAR(64) PRIMARY KEY, lock_until TIMESTAMP NOT NULL, locked_at TIMESTAMP NOT NULL, locked_by VARCHAR(255) NOT NULL)");

        sourceJdbcTemplate.execute("DELETE FROM orders");
        targetJdbcTemplate.execute("DELETE FROM orders_target");
        targetJdbcTemplate.execute("DELETE FROM sync_checkpoint");

        // SourceDB에 5건 삽입
        for (long i = 1; i <= 5; i++) {
            sourceJdbcTemplate.update(
                "INSERT INTO orders (id, order_no, customer_id, status, amount, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                i, "ORD-" + i, 1000 + i, "CREATED", new BigDecimal("100.00"), Timestamp.valueOf(LocalDateTime.now())
            );
        }
    }

    @Test
    void execute_전체동기화흐름이_정상적으로_동작한다() {
        // when
        syncJobExecutor.execute();

        // then
        Integer count = targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders_target", Integer.class);
        assertThat(count).isEqualTo(5);

        Long lastId = targetJdbcTemplate.queryForObject("SELECT last_id FROM sync_checkpoint WHERE job_name = 'ORDERS_SYNC'", Long.class);
        assertThat(lastId).isEqualTo(5L);
        
        Long processedCnt = targetJdbcTemplate.queryForObject("SELECT processed_cnt FROM sync_checkpoint WHERE job_name = 'ORDERS_SYNC'", Long.class);
        assertThat(processedCnt).isEqualTo(5L);
    }
}
