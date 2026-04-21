package com.example.sync.infrastructure;

import com.example.sync.service.SyncJobExecutor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

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
        lockAtMostFor  = "PT10M",
        lockAtLeastFor = "PT10S"
    )
    public void runSyncJob() {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        MDC.put("instance", System.getenv().getOrDefault("HOSTNAME",
                            System.getenv().getOrDefault("POD_NAME", "local")));
        try {
            log.info("[SyncJob] tick 시작");
            executor.execute();
        } finally {
            MDC.clear();
        }
    }
}
