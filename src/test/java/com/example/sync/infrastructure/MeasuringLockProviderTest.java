package com.example.sync.infrastructure;

import com.example.sync.service.monitoring.SyncMetrics;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeasuringLockProviderTest {

    @Mock private LockProvider delegate;
    @Mock private SyncMetrics metrics;
    @Mock private SimpleLock mockLock;

    private MeasuringLockProvider measuringLockProvider;

    private LockConfiguration lockConfig() {
        return new LockConfiguration(Instant.now(), "TEST_LOCK",
            java.time.Duration.ofMinutes(10), java.time.Duration.ofSeconds(10));
    }

    @Test
    void lock_성공시_incrementLockAcquired_호출() {
        when(delegate.lock(any())).thenReturn(Optional.of(mockLock));
        measuringLockProvider = new MeasuringLockProvider(delegate, metrics);

        Optional<SimpleLock> result = measuringLockProvider.lock(lockConfig());

        assertThat(result).isPresent();
        verify(metrics).incrementLockAcquired();
        verify(metrics, never()).incrementLockFailed();
    }

    @Test
    void lock_실패시_incrementLockFailed_호출() {
        when(delegate.lock(any())).thenReturn(Optional.empty());
        measuringLockProvider = new MeasuringLockProvider(delegate, metrics);

        Optional<SimpleLock> result = measuringLockProvider.lock(lockConfig());

        assertThat(result).isEmpty();
        verify(metrics).incrementLockFailed();
        verify(metrics, never()).incrementLockAcquired();
    }

    @Test
    void lock_delegate_결과를_그대로_반환() {
        when(delegate.lock(any())).thenReturn(Optional.of(mockLock));
        measuringLockProvider = new MeasuringLockProvider(delegate, metrics);

        Optional<SimpleLock> result = measuringLockProvider.lock(lockConfig());

        assertThat(result.get()).isSameAs(mockLock);
    }
}
