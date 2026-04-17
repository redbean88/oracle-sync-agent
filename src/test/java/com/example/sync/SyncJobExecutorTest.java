package com.example.sync;

import com.example.sync.infra.batch.SyncJobExecutor;
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
public class SyncJobExecutorTest extends OracleTestBase {

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
        // Oracle 스키마 초기화
        targetJdbcTemplate.execute("BEGIN EXECUTE IMMEDIATE 'DROP TABLE orders'; EXCEPTION WHEN OTHERS THEN NULL; END;");
        targetJdbcTemplate.execute("BEGIN EXECUTE IMMEDIATE 'DROP TABLE orders_target'; EXCEPTION WHEN OTHERS THEN NULL; END;");
        targetJdbcTemplate.execute("BEGIN EXECUTE IMMEDIATE 'DROP TABLE sync_checkpoint'; EXCEPTION WHEN OTHERS THEN NULL; END;");
        targetJdbcTemplate.execute("BEGIN EXECUTE IMMEDIATE 'DROP TABLE batch_retry_queue'; EXCEPTION WHEN OTHERS THEN NULL; END;");
        targetJdbcTemplate.execute("BEGIN EXECUTE IMMEDIATE 'DROP TABLE shedlock'; EXCEPTION WHEN OTHERS THEN NULL; END;");

        targetJdbcTemplate.execute("CREATE TABLE orders (id NUMBER PRIMARY KEY, order_no VARCHAR2(50), customer_id NUMBER, status VARCHAR2(20), amount NUMBER(10,2), created_at TIMESTAMP)");
        targetJdbcTemplate.execute("CREATE TABLE orders_target (id NUMBER PRIMARY KEY, order_no VARCHAR2(50), customer_id NUMBER, status VARCHAR2(20), amount NUMBER(10,2), created_at TIMESTAMP)");
        targetJdbcTemplate.execute("CREATE TABLE sync_checkpoint (job_name VARCHAR2(100) PRIMARY KEY, last_id NUMBER DEFAULT 0, last_synced_at TIMESTAMP, processed_cnt NUMBER DEFAULT 0)");
        targetJdbcTemplate.execute("CREATE TABLE batch_retry_queue (id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, source_ids CLOB, error_type VARCHAR2(100), error_message CLOB, retry_count NUMBER DEFAULT 0, max_retry NUMBER DEFAULT 5, next_retry_at TIMESTAMP, status VARCHAR2(20) DEFAULT 'PENDING')");
        targetJdbcTemplate.execute("CREATE TABLE shedlock (name VARCHAR2(64) PRIMARY KEY, lock_until TIMESTAMP NOT NULL, locked_at TIMESTAMP NOT NULL, locked_by VARCHAR2(255) NOT NULL)");

        // 데이터 삽입
        for (long i = 1; i <= 5; i++) {
            targetJdbcTemplate.update(
                "INSERT INTO orders (id, order_no, customer_id, status, amount, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                i, "ORD-" + i, 1000 + i, "CREATED", new BigDecimal("100.00"), Timestamp.valueOf(LocalDateTime.now())
            );
        }
    }

    @Test
    void execute_전체동기화흐름이_정상적으로_동작한다() {
        syncJobExecutor.execute();

        Integer count = targetJdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders_target", Integer.class);
        assertThat(count).isEqualTo(5);

        Long lastId = targetJdbcTemplate.queryForObject("SELECT last_id FROM sync_checkpoint WHERE job_name = 'ORDERS_SYNC'", Long.class);
        assertThat(lastId).isEqualTo(5L);
    }
}
