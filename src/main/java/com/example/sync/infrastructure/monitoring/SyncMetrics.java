package com.example.sync.infrastructure.monitoring;

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

    @PostConstruct
    void init() {
        readTimer  = registry.timer("sync.read.duration");
        writeTimer = registry.timer("sync.write.duration");
        lagGauge   = registry.gauge("sync.lag.count", new AtomicLong(0));
        registry.counter("sync.read.error");
        registry.counter("sync.write.error");
    }

    public void recordReadTime(long ms)  { readTimer.record(ms, TimeUnit.MILLISECONDS); }
    public void recordWriteTime(long ms) { writeTimer.record(ms, TimeUnit.MILLISECONDS); }
    public void recordLag(long lag)      { lagGauge.set(lag); }
    public void incrementReadError()     { registry.counter("sync.read.error").increment(); }
    public void incrementWriteError()    { registry.counter("sync.write.error").increment(); }
}
