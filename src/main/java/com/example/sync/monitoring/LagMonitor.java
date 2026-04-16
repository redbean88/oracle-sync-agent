package com.example.sync.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LagMonitor {

    @Qualifier("sourceJdbcTemplate")
    private final JdbcTemplate sourceJdbcTemplate;
    
    private final AlertService alertService;
    private final SyncMetrics metrics;

    @Value("${sync.lag-alert-threshold:100000}")
    private long lagAlertThreshold;

    public void check(String jobName, long checkpointId) {
        Long sourceMaxId = sourceJdbcTemplate.queryForObject(
            "SELECT MAX(id) FROM orders", Long.class);

        if (sourceMaxId == null) return;

        long lag = sourceMaxId - checkpointId;
        metrics.recordLag(lag);
        log.info("[Lag] sourceMaxId={} checkpointId={} lag={}", sourceMaxId, checkpointId, lag);

        if (lag > lagAlertThreshold) {
            alertService.sendSlack(
                String.format("⚠️ [%s] Lag 임계치 초과: %,d 건 밀림", jobName, lag));
        }
    }
}
