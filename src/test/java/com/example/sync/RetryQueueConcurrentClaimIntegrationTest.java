package com.example.sync;

import com.example.sync.service.retry.BatchRetryQueue;
import com.example.sync.service.retry.RetryQueueRepository;
import com.example.sync.service.retry.RetryStatus;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * FOR UPDATE SKIP LOCKED 기반 중복 CLAIM 방지 검증.
 */
@SpringBootTest
class RetryQueueConcurrentClaimIntegrationTest extends AbstractOracleIntegrationTest {

    @Autowired private RetryQueueRepository repo;
    @Autowired @Qualifier("proxyJdbcTemplate") private JdbcTemplate proxyJdbc;

    @BeforeEach
    void setUp() {
        LockAssert.TestHelper.makeAllAssertsPass(true);
        proxyJdbc.execute("DELETE FROM batch_retry_queue");
    }

    @Test
    void 두스레드_동시_claim_교집합없음() throws InterruptedException {
        // 50건 PENDING 등록 (backoffSec=0 → 즉시 claim 가능)
        for (int i = 0; i < 50; i++) {
            repo.save(buildEntry(String.valueOf(i + 1)), 0);
        }

        List<Long> claimed1 = new CopyOnWriteArrayList<>();
        List<Long> claimed2 = new CopyOnWriteArrayList<>();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        pool.submit(() -> {
            try {
                start.await();
                claimed1.addAll(repo.claimPending(20).stream()
                    .map(BatchRetryQueue::getId).collect(Collectors.toList()));
            } catch (Exception ignored) {}
            finally { done.countDown(); }
        });
        pool.submit(() -> {
            try {
                start.await();
                claimed2.addAll(repo.claimPending(20).stream()
                    .map(BatchRetryQueue::getId).collect(Collectors.toList()));
            } catch (Exception ignored) {}
            finally { done.countDown(); }
        });

        start.countDown();
        assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        // 교집합 없음
        assertThat(claimed1).doesNotContainAnyElementsOf(claimed2);
        // 합계 50 이하
        assertThat(claimed1.size() + claimed2.size()).isLessThanOrEqualTo(50);
    }

    @Test
    void backoffSec0_즉시_claim_가능() {
        repo.save(buildEntry("1"), 0);

        List<BatchRetryQueue> claimed = repo.claimPending(10);

        assertThat(claimed).isNotEmpty();
    }

    @Test
    void backoffSec3600_claim_불가() {
        repo.save(buildEntry("2"), 3600);

        List<BatchRetryQueue> claimed = repo.claimPending(10);

        assertThat(claimed).isEmpty();
    }

    private static BatchRetryQueue buildEntry(String sourceId) {
        return BatchRetryQueue.builder()
            .sourceIds(sourceId)
            .errorType("RuntimeException")
            .errorMessage("테스트 오류")
            .maxRetry(3)
            .status(RetryStatus.PENDING)
            .build();
    }
}
