package com.example.sync.service.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import javax.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SyncMetrics {

    private final MeterRegistry registry;
    private Timer readTimer;
    private Timer writeTimer;
    private AtomicLong lagGauge;

    public SyncMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    private Timer tickTimer;

    @PostConstruct
    void init() {
        readTimer  = registry.timer("sync.read.duration");
        writeTimer = registry.timer("sync.write.duration");
        tickTimer  = registry.timer("sync.tick.duration");
        lagGauge   = registry.gauge("sync.lag.count", new AtomicLong(0));
        registry.counter("sync.read.error");
        registry.counter("sync.write.error");
        registry.counter("sync.checkpoint.regression_skip");
        registry.counter("sync.retry.claim_zero");
        registry.counter("sync.lock.acquisition_success");
        registry.counter("sync.lock.acquisition_failure");
    }

    public void recordReadTime(long ms)  { readTimer.record(ms, TimeUnit.MILLISECONDS); }
    public void recordWriteTime(long ms) { writeTimer.record(ms, TimeUnit.MILLISECONDS); }
    public void recordTickDuration(long ms) { tickTimer.record(ms, TimeUnit.MILLISECONDS); }
    public void recordLag(long lag)      { lagGauge.set(lag); }
    public void incrementReadError()          { registry.counter("sync.read.error").increment(); }
    public void incrementCheckpointRegression() { registry.counter("sync.checkpoint.regression_skip").increment(); }
    public void incrementRetryClaimZero()     { registry.counter("sync.retry.claim_zero").increment(); }
    public void incrementLockAcquired()       { registry.counter("sync.lock.acquisition_success").increment(); }
    public void incrementLockFailed()         { registry.counter("sync.lock.acquisition_failure").increment(); }
}
