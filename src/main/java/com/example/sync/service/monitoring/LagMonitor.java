package com.example.sync.service.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LagMonitor {

    private static final Logger log = LoggerFactory.getLogger(LagMonitor.class);

    private final AlertService alertService;
    private final SyncMetrics metrics;

    public LagMonitor(AlertService alertService, SyncMetrics metrics) {
        this.alertService = alertService;
        this.metrics = metrics;
    }

    /**
     * 어댑터에서 계산한 lag을 기록하고 임계치 초과 시 알림 발송.
     * DB 쿼리는 어댑터의 checkLag()에서 수행.
     */
    public void recordAndAlert(String jobName, long lag, long threshold) {
        metrics.recordLag(jobName, lag);
        log.info("[Lag] job={} lag={} threshold={}", jobName, lag, threshold);
        if (lag > threshold) {
            alertService.sendAlert(
                    String.format("⚠️ [%s] Lag 임계치 초과: %,d 건 밀림", jobName, lag));
        }
    }
}
