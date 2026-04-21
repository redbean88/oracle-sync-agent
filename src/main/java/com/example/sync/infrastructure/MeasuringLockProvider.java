package com.example.sync.infrastructure;

import com.example.sync.service.monitoring.SyncMetrics;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;

import java.util.Optional;

/**
 * LockProvider 데코레이터 — 락 획득 성공/실패를 SyncMetrics에 계측.
 * lock.acquisition_failure 급증 시 split-brain 의심 경보 트리거 기준으로 활용.
 */
public class MeasuringLockProvider implements LockProvider {

    private final LockProvider delegate;
    private final SyncMetrics metrics;

    public MeasuringLockProvider(LockProvider delegate, SyncMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics;
    }

    @Override
    public Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {
        Optional<SimpleLock> result = delegate.lock(lockConfiguration);
        if (result.isPresent()) {
            metrics.incrementLockAcquired();
        } else {
            metrics.incrementLockFailed();
        }
        return result;
    }
}
