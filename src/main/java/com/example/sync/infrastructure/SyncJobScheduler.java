package com.example.sync.infrastructure;

import com.example.sync.service.SyncJobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SyncJobScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncJobScheduler.class);

    private final SyncJobExecutor executor;

    public SyncJobScheduler(SyncJobExecutor executor) {
        this.executor = executor;
    }

    @Scheduled(fixedDelayString = "${sync.fixed-delay-ms:60000}")
    @SchedulerLock(
        name = "ORDERS_SYNC_LOCK",
        lockAtMostFor  = "PT55S",
        lockAtLeastFor = "PT10S"
    )
    public void runSyncJob() {
        log.info("[SyncJob] tick 시작");
        executor.execute();
    }
}
