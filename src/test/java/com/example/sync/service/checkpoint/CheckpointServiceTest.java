package com.example.sync.service.checkpoint;

import com.example.sync.repository.SyncCheckpointRepository;
import com.example.sync.service.monitoring.SyncMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckpointServiceTest {

    @Mock
    private SyncCheckpointRepository repository;

    @Mock
    private SyncMetrics metrics;

    @InjectMocks
    private CheckpointService service;

    @Test
    void update_bumpForward_성공시_metric_미호출() {
        when(repository.bumpForward(eq("JOB"), eq(200L), eq(10L), eq(2000))).thenReturn(1);

        service.update("JOB", 200L, 10, 2000);

        verify(metrics, never()).incrementCheckpointRegression();
        verify(repository, never()).insert(any(), anyLong(), anyLong(), any());
    }

    @Test
    void update_역진감지_regression_metric_호출() {
        when(repository.bumpForward(eq("JOB"), eq(50L), eq(10L), eq(2000))).thenReturn(0);
        when(repository.existsById("JOB")).thenReturn(true);

        service.update("JOB", 50L, 10, 2000);

        verify(metrics).incrementCheckpointRegression();
        verify(repository, never()).insert(any(), anyLong(), anyLong(), any());
    }

    @Test
    void update_신규레코드_insert_호출() {
        when(repository.bumpForward(eq("JOB"), eq(100L), eq(5L), eq(2000))).thenReturn(0);
        when(repository.existsById("JOB")).thenReturn(false);

        service.update("JOB", 100L, 5, 2000);

        verify(repository).insert(eq("JOB"), eq(100L), eq(5L), eq(2000));
        verify(metrics, never()).incrementCheckpointRegression();
    }

    @Test
    void getLastId_레코드_없으면_0반환() {
        when(repository.findLastId("JOB")).thenReturn(0L);
        assertThat(service.getLastId("JOB")).isEqualTo(0L);
    }

    private static org.assertj.core.api.AbstractLongAssert<?> assertThat(long actual) {
        return org.assertj.core.api.Assertions.assertThat(actual);
    }
}
