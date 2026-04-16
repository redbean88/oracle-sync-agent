package com.example.sync.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SyncJobScheduler {

    private final SyncJobExecutor executor;

    // fixedDelay: 이전 실행 완료 후 주기 대기 (fixedRate 금지 — 중첩 실행 위험)
    @Scheduled(fixedDelayString = "${sync.fixed-delay-ms:60000}")
    @SchedulerLock(
        name = "ORDERS_SYNC_LOCK",
        lockAtMostFor  = "PT55S",   // 비정상 종료 시 55초 후 자동 해제
        lockAtLeastFor = "PT10S"    // 빠른 재실행 방지 (최소 10초)
    )
    public void runSyncJob() {
        log.info("[SyncJob] tick 시작");
        executor.execute();
    }
}
