package com.example.sync;

import com.example.sync.service.SyncJobExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SyncIntegrationTest {

    @Autowired
    private SyncJobExecutor syncJobExecutor;

    @Autowired
    @Qualifier("sourceJdbcTemplate")
    private JdbcTemplate sourceJdbcTemplate;

    @Autowired
    @Qualifier("targetJdbcTemplate")
    private JdbcTemplate targetJdbcTemplate;

    @Autowired
    @Qualifier("proxyJdbcTemplate")
    private JdbcTemplate proxyJdbcTemplate;

    @BeforeEach
    void setUp() {
        // Source DB 초기화
        try { sourceJdbcTemplate.execute("DROP TABLE orders"); } catch (Exception ignored) {}
        sourceJdbcTemplate.execute("CREATE TABLE orders (id NUMBER PRIMARY KEY, order_no VARCHAR2(50), customer_id NUMBER, status VARCHAR2(20), amount NUMBER(10,2), created_at TIMESTAMP)");
        
        // Target DB 초기화
        try { targetJdbcTemplate.execute("DROP TABLE orders_target"); } catch (Exception ignored) {}
        targetJdbcTemplate.execute("CREATE TABLE orders_target (id NUMBER PRIMARY KEY, order_no VARCHAR2(50), customer_id NUMBER, status VARCHAR2(20), amount NUMBER(10,2), created_at TIMESTAMP)");

        // Proxy DB 초기화 (Metadata)
        try { proxyJdbcTemplate.execute("DROP TABLE sync_checkpoint"); } catch (Exception ignored) {}
        try { proxyJdbcTemplate.execute("DROP TABLE batch_retry_queue"); } catch (Exception ignored) {}
        try { proxyJdbcTemplate.execute("DROP TABLE shedlock"); } catch (Exception ignored) {}

        proxyJdbcTemplate.execute("CREATE TABLE sync_checkpoint (job_name VARCHAR2(100) PRIMARY KEY, last_id NUMBER DEFAULT 0, last_synced_at TIMESTAMP, processed_cnt NUMBER DEFAULT 0)");
        proxyJdbcTemplate.execute("CREATE TABLE batch_retry_queue (id NUMBER GENERATED AS IDENTITY PRIMARY KEY, source_ids CLOB, error_type VARCHAR2(100), error_message CLOB, retry_count NUMBER DEFAULT 0, max_retry NUMBER DEFAULT 5, next_retry_at TIMESTAMP, status VARCHAR2(20) DEFAULT 'PENDING')");
        proxyJdbcTemplate.execute("CREATE TABLE shedlock (name VARCHAR2(64) PRIMARY KEY, lock_until TIMESTAMP, locked_at TIMESTAMP, locked_by VARCHAR2(255))");

        // 소스 데이터 삽입
        for (long i = 1; i <= 3; i++) {
            sourceJdbcTemplate.update("INSERT INTO orders (id, order_no, customer_id, status, amount, created_at) VALUES (?, ?, ?, 'CREATED', 100.0, ?)",
                    i, "ORD-" + i, 1000 + i, Timestamp.valueOf(LocalDateTime.now()));
        }
    }

    @Test
    void execute_로컬오라클환경에서도_전체동기화가_정상동작한다() {
        // Sync 실행
        syncJobExecutor.execute();

        // Target 검증
        Integer targetCount = targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders_target", Integer.class);
        assertThat(targetCount).isEqualTo(3);

        // Proxy(Metadata) 검증
        Long lastId = proxyJdbcTemplate.queryForObject("SELECT last_id FROM sync_checkpoint WHERE job_name = 'ORDERS_SYNC'", Long.class);
        assertThat(lastId).isEqualTo(3L);
    }
}
