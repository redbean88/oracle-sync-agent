package com.example.sync;

import com.example.sync.infrastructure.config.RetryConfig;
import com.example.sync.infrastructure.batch.AdaptiveChunkSizer;
import com.example.sync.service.SyncJobExecutor;
import com.example.sync.service.adapter.OrdersSyncAdapter;
import com.example.sync.dto.OrdersRecordDto;
import com.example.sync.service.monitoring.SyncMetrics;
import com.example.sync.service.retry.DeadLetterHandler;
import com.example.sync.service.retry.RetryQueueProcessor;
import com.example.sync.service.retry.RetryQueueRepository;
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
    @Autowired private OrdersSyncAdapter ordersAdapter;
    @Autowired private RetryQueueRepository retryQueueRepository;
    @Autowired private DeadLetterHandler deadLetterHandler;
    @Autowired private SyncMetrics syncMetrics;
    @Autowired @Qualifier("sourceJdbcTemplate") private JdbcTemplate sourceJdbc;
    @Autowired @Qualifier("targetJdbcTemplate") private JdbcTemplate targetJdbc;
    @Autowired @Qualifier("proxyJdbcTemplate")  private JdbcTemplate proxyJdbc;
    @Autowired private MeterRegistry meterRegistry;

    private AdaptiveChunkSizer chunkSizer;
    private RetryQueueProcessor<OrdersRecordDto> retryProcessor;

    @BeforeEach
    void setUp() {
        LockAssert.TestHelper.makeAllAssertsPass(true);

        chunkSizer = new AdaptiveChunkSizer(100, 10, 500);
        RetryConfig retryConfig = new RetryConfig();
        retryProcessor = new RetryQueueProcessor<>(ordersAdapter, retryQueueRepository, deadLetterHandler, syncMetrics, retryConfig);

        // FK 의존 순서: orders_target → orders (orders_target이 orders를 참조할 수 있음)
        try { targetJdbc.execute("DELETE FROM orders_target"); }   catch (Exception ignored) {}
        try { sourceJdbc.execute("DELETE FROM orders"); }          catch (Exception ignored) {}
        try { proxyJdbc.execute("DELETE FROM sync_checkpoint"); }  catch (Exception ignored) {}
        try { proxyJdbc.execute("DELETE FROM batch_retry_queue"); } catch (Exception ignored) {}

        String runId = String.valueOf(System.currentTimeMillis() % 100000);
        for (int i = 0; i < 3; i++) {
            sourceJdbc.update(
                "INSERT INTO orders (id, order_no, customer_id, status, amount, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                    i + 1,"ORD-" + runId + "-" + (i + 1), 1000L + i, "CREATED", 100.0 + i, Timestamp.valueOf(LocalDateTime.now())
            );
        }
    }

    @Test
    void execute_전체동기화_3건_정상처리() {
        syncJobExecutor.execute(ordersAdapter, chunkSizer, retryProcessor, 100000L);

        Integer targetCount = targetJdbc.queryForObject("SELECT COUNT(*) FROM orders_target", Integer.class);
        assertThat(targetCount).isEqualTo(3);
    }

    @Test
    void execute_후_retry_queue_PENDING_없음() {
        syncJobExecutor.execute(ordersAdapter, chunkSizer, retryProcessor, 100000L);

        Integer pendingCount = proxyJdbc.queryForObject(
            "SELECT COUNT(*) FROM batch_retry_queue WHERE status = 'PENDING'", Integer.class);
        assertThat(pendingCount).isZero();
    }

    @Test
    void execute_후_tick_duration_메트릭_등록됨() {
        syncJobExecutor.execute(ordersAdapter, chunkSizer, retryProcessor, 100000L);

        boolean hasTick = meterRegistry.getMeters().stream()
            .anyMatch(m -> m.getId().getName().equals("sync.tick.duration"));
        assertThat(hasTick).isTrue();
    }
}
