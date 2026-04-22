package com.example.sync.service.retry;

import com.example.sync.service.monitoring.AlertService;
import com.example.sync.service.monitoring.SyncMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DeadLetterHandler {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterHandler.class);

    private final RetryQueueRepository repo;
    private final AlertService alertService;
    private final SyncMetrics metrics;

    private final ConcurrentHashMap<String, AtomicInteger> deadCountByJob = new ConcurrentHashMap<>();

    public DeadLetterHandler(RetryQueueRepository repo, AlertService alertService, SyncMetrics metrics) {
        this.repo = repo;
        this.alertService = alertService;
        this.metrics = metrics;
    }

    public void handle(BatchRetryQueue item) {
        repo.markDead(item.getId());
        metrics.incrementRetryDead(item.getJobName() != null ? item.getJobName() : "UNKNOWN");
        deadCountByJob
                .computeIfAbsent(item.getJobName() != null ? item.getJobName() : "UNKNOWN",
                                  k -> new AtomicInteger())
                .incrementAndGet();
        log.error("[DeadLetter][{}] 재시도 한도 초과: ID={}, Error={}", item.getJobName(), item.getId(), item.getErrorType());
    }

    /** 1분마다 job별 DLQ 누적 건수를 묶어 알림 발송 (in-memory, 인스턴스별 독립) */
    @Scheduled(fixedDelay = 60_000)
    public void flushAlerts() {
        deadCountByJob.forEach((job, count) -> {
            int n = count.getAndSet(0);
            if (n > 0) {
                alertService.sendAlert(String.format("[%s] DLQ %d건 발생 (지난 1분)", job, n));
            }
        });
    }
}
