package com.example.sync;

import com.example.sync.service.checkpoint.CheckpointService;
import com.example.sync.service.monitoring.SyncMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * split-brain 시뮬레이션 — checkpoint 단조 증가 보장 검증.
 */
@SpringBootTest
class CheckpointMonotonicIntegrationTest extends AbstractOracleIntegrationTest {

    @Autowired private CheckpointService checkpointService;
    @Autowired @Qualifier("proxyJdbcTemplate") private JdbcTemplate proxyJdbc;
    @Autowired private SyncMetrics metrics;
    @Autowired private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        LockAssert.TestHelper.makeAllAssertsPass(true);
        proxyJdbc.execute("DELETE FROM sync_checkpoint");
    }

    @Test
    void 동시_update_큰값이_최종저장된다() throws InterruptedException {
        // 사전 레코드 삽입 — 없으면 두 스레드 모두 INSERT 경쟁이 발생해 마지막 스레드가 이김
        proxyJdbc.update(
            "INSERT INTO sync_checkpoint (job_name, last_id) VALUES (?, 0)", "MONO_JOB");

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        pool.submit(() -> {
            try {
                start.await();
                checkpointService.update("MONO_JOB", 200L, 10, 2000);
            } catch (Exception ignored) {}
            finally { done.countDown(); }
        });
        pool.submit(() -> {
            try {
                start.await();
                checkpointService.update("MONO_JOB", 100L, 10, 2000);
            } catch (Exception ignored) {}
            finally { done.countDown(); }
        });

        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(checkpointService.getLastId("MONO_JOB")).isEqualTo(200L);
    }

    @Test
    void 역진_update_무시되고_기존값_유지() {
        checkpointService.update("BACK_JOB", 200L, 10, 2000);
        checkpointService.update("BACK_JOB", 150L, 10, 2000);  // 역진 시도

        assertThat(checkpointService.getLastId("BACK_JOB")).isEqualTo(200L);
    }

    @Test
    void 역진시_regression_metric_증가() {
        checkpointService.update("REG_JOB", 300L, 10, 2000);

        double before = regressionCount();
        checkpointService.update("REG_JOB", 100L, 10, 2000);  // 역진
        double after = regressionCount();

        assertThat(after).isGreaterThan(before);
    }

    private double regressionCount() {
        Counter counter = meterRegistry.find("sync.checkpoint.regression_skip").counter();
        return counter != null ? counter.count() : 0.0;
    }
}
