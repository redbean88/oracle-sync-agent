package com.example.sync.service;

import com.example.sync.infrastructure.config.RetryConfig;
import com.example.sync.infrastructure.batch.AdaptiveChunkSizer;
import com.example.sync.service.adapter.OrdersSyncAdapter;
import com.example.sync.dto.OrdersRecordDto;
import com.example.sync.service.monitoring.SyncMetrics;
import com.example.sync.service.retry.DeadLetterHandler;
import com.example.sync.service.retry.RetryQueueProcessor;
import com.example.sync.service.retry.RetryQueueRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "sync.orders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrdersSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrdersSyncScheduler.class);

    @Autowired private SyncJobExecutor executor;
    @Autowired private OrdersSyncAdapter adapter;
    @Autowired private RetryQueueRepository retryRepo;
    @Autowired private DeadLetterHandler deadLetterHandler;
    @Autowired private SyncMetrics metrics;

    @Value("${sync.orders.chunk-size-initial:2000}") private int chunkSizeInitial;
    @Value("${sync.orders.chunk-size-min:500}")      private int chunkSizeMin;
    @Value("${sync.orders.chunk-size-max:10000}")    private int chunkSizeMax;
    @Value("${sync.orders.lag-alert-threshold:100000}") private long lagAlertThreshold;
    @Value("${sync.orders.retry.max-retry:3}")             private int maxRetry;
    @Value("${sync.orders.retry.initial-backoff-sec:60}")  private int initialBackoffSec;
    @Value("${sync.orders.retry.backoff-step-sec:300}")    private int backoffStepSec;
    @Value("${sync.orders.retry.claim-batch-size:50}")     private int claimBatchSize;

    private AdaptiveChunkSizer chunkSizer;
    private RetryQueueProcessor<OrdersRecordDto> retryProcessor;

    @PostConstruct
    void init() {
        chunkSizer = new AdaptiveChunkSizer(chunkSizeInitial, chunkSizeMin, chunkSizeMax);
        RetryConfig cfg = new RetryConfig(maxRetry, initialBackoffSec, backoffStepSec, claimBatchSize);
        retryProcessor = new RetryQueueProcessor<>(adapter, retryRepo, deadLetterHandler, metrics, cfg);
    }

    @Scheduled(fixedDelayString = "${sync.orders.fixed-delay-ms:60000}")
    @SchedulerLock(name = "ORDERS_SYNC", lockAtMostFor = "PT10M", lockAtLeastFor = "PT10S")
    public void tick() {
        MDC.put("traceId", UUID.randomUUID().toString().substring(0, 8));
        MDC.put("jobName", "ORDERS_SYNC");
        MDC.put("instance", System.getenv().getOrDefault("HOSTNAME",
                            System.getenv().getOrDefault("POD_NAME", "local")));
        try {
            executor.execute(adapter, chunkSizer, retryProcessor, lagAlertThreshold);
        } catch (Throwable t) {
            log.error("[ORDERS_SYNC] tick 실행 중 오류", t);
        } finally {
            MDC.clear();
        }
    }
}
