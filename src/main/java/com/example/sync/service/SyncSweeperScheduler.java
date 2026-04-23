package com.example.sync.service;

import com.example.sync.service.monitoring.SyncMetrics;
import com.example.sync.service.retry.RetryQueueRepository;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SyncSweeperScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncSweeperScheduler.class);

    @Autowired private RetryQueueRepository retryRepo;
    @Autowired private SyncMetrics metrics;

    @Scheduled(fixedDelay = 300_000L)
    @SchedulerLock(name = "REAP_STUCK_LOCK", lockAtMostFor = "PT5M", lockAtLeastFor = "PT10S")
    public void sweep() {
        try {
            int reaped = retryRepo.reapStuckProcessing(900);
            int purged = retryRepo.purgeOldSuccess(604800);
            if (reaped > 0) {
                log.warn("[Sweeper] PROCESSING stuck {} 건 PENDING으로 복구", reaped);
                metrics.incrementStuckReaped(reaped);
            }
            if (purged > 0) {
                log.info("[Sweeper] SUCCESS {} 건 정리 완료", purged);
            }
        } catch (Throwable t) {
            log.error("[Sweeper] 오류", t);
        }
    }
}
