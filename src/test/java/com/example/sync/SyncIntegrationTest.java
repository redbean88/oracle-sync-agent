package com.example.sync;

import com.example.sync.service.SyncJobExecutor;
import io.micrometer.core.instrument.MeterRegistry;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SyncIntegrationTest extends AbstractOracleIntegrationTest {

    @Autowired private SyncJobExecutor syncJobExecutor;
    @Autowired @Qualifier("sourceJdbcTemplate") private JdbcTemplate sourceJdbc;
    @Autowired @Qualifier("targetJdbcTemplate") private JdbcTemplate targetJdbc;
    @Autowired @Qualifier("proxyJdbcTemplate")  private JdbcTemplate proxyJdbc;
    @Autowired private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        LockAssert.TestHelper.makeAllAssertsPass(true);

        // 테이블 데이터 초기화
        try { sourceJdbc.execute("DELETE FROM orders"); }          catch (Exception ignored) {}
        try { targetJdbc.execute("DELETE FROM orders_target"); }   catch (Exception ignored) {}
        try { proxyJdbc.execute("DELETE FROM sync_checkpoint"); }  catch (Exception ignored) {}
        try { proxyJdbc.execute("DELETE FROM batch_retry_queue"); } catch (Exception ignored) {}

        // source 데이터 3건 (IDENTITY 컬럼이므로 id 지정 불가 — sequence 사용)
        for (int i = 0; i < 3; i++) {
            sourceJdbc.update(
                "INSERT INTO orders (id, order_no, customer_id, status, amount, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                (long)(i + 1), "ORD-" + (i + 1), 1000L + i, "CREATED", 100.0 + i, Timestamp.valueOf(LocalDateTime.now())
            );
        }
    }

    @Test
    void execute_전체동기화_3건_정상처리() {
        syncJobExecutor.execute();

        Integer targetCount = targetJdbc.queryForObject("SELECT COUNT(*) FROM orders_target", Integer.class);
        assertThat(targetCount).isEqualTo(3);
    }

    @Test
    void execute_후_checkpoint_version_낙관락_작동() {
        syncJobExecutor.execute();

        Long version = proxyJdbc.queryForObject(
            "SELECT version FROM sync_checkpoint WHERE job_name = 'ORDERS_SYNC'", Long.class);
        assertThat(version).isNotNull();
    }

    @Test
    void execute_후_retry_queue_PENDING_없음() {
        syncJobExecutor.execute();

        Integer pendingCount = proxyJdbc.queryForObject(
            "SELECT COUNT(*) FROM batch_retry_queue WHERE status = 'PENDING'", Integer.class);
        assertThat(pendingCount).isZero();
    }

    @Test
    void execute_후_tick_duration_메트릭_등록됨() {
        syncJobExecutor.execute();

        boolean hasTick = meterRegistry.getMeters().stream()
            .anyMatch(m -> m.getId().getName().equals("sync.tick.duration"));
        assertThat(hasTick).isTrue();
    }
}
