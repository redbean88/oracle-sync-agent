package com.example.sync.service.monitoring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LagMonitorTest {

    @InjectMocks
    LagMonitor lagMonitor;

    @Mock
    AlertService alertService;

    @Mock
    SyncMetrics metrics;

    @Test
    void recordAndAlert_noAlert() {
        lagMonitor.recordAndAlert("TEST", -1, 100000);
        verify(alertService, never()).sendAlert(anyString());
    }

    @Test
    void recordAndAlert_alert() {
        lagMonitor.recordAndAlert("TEST", 1_000_000, 100000);
        verify(alertService).sendAlert(anyString());
    }
}
