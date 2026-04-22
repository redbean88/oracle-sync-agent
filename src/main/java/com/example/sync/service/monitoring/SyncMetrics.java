package com.example.sync.service.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SyncMetrics {

    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, AtomicLong> lagGauges = new ConcurrentHashMap<>();

    public SyncMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordReadTime(String jobName, long ms) {
        registry.timer("sync.read.duration", "job", jobName).record(ms, TimeUnit.MILLISECONDS);
    }

    public void recordWriteTime(String jobName, long ms) {
        registry.timer("sync.write.duration", "job", jobName).record(ms, TimeUnit.MILLISECONDS);
    }

    public void recordTickDuration(String jobName, long ms) {
        registry.timer("sync.tick.duration", "job", jobName).record(ms, TimeUnit.MILLISECONDS);
    }

    public void recordLag(String jobName, long lag) {
        lagGauges.computeIfAbsent(jobName, jn -> {
            AtomicLong g = new AtomicLong(0);
            registry.gauge("sync.lag.count", Tags.of("job", jn), g);
            return g;
        }).set(lag);
    }

    public void incrementReadError(String jobName) {
        registry.counter("sync.read.error", "job", jobName).increment();
    }

    public void incrementCheckpointRegression() {
        registry.counter("sync.checkpoint.regression_skip").increment();
    }

    public void incrementRetryClaimZero(String jobName) {
        registry.counter("sync.retry.claim_zero", "job", jobName).increment();
    }

    public void incrementRetrySuccess(String jobName) {
        registry.counter("sync.retry.success.count", "job", jobName).increment();
    }

    public void incrementRetryDead(String jobName) {
        registry.counter("sync.retry.dead.count", "job", jobName).increment();
    }

    public void incrementStuckReaped(int count) {
        registry.counter("sync.retry.processing.stuck").increment(count);
    }

    public void incrementLockAcquired() {
        registry.counter("sync.lock.acquisition_success").increment();
    }

    public void incrementLockFailed() {
        registry.counter("sync.lock.acquisition_failure").increment();
    }
}
