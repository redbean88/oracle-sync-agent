package com.example.sync.batch;

import com.example.sync.infrastructure.batch.AdaptiveChunkSizer;
import com.example.sync.service.SyncJobExecutor;
import com.example.sync.service.adapter.SyncJobAdapter;
import com.example.sync.service.checkpoint.CheckpointService;
import com.example.sync.service.monitoring.SyncMetrics;
import com.example.sync.service.retry.RetryQueueProcessor;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SyncJobExecutorMockTest {

    @Mock private CheckpointService checkpointService;
    @Mock private SyncMetrics metrics;
    @Mock private SyncJobAdapter<Object> adapter;
    @SuppressWarnings("unchecked")
    @Mock private RetryQueueProcessor<Object> retryProcessor;

    private SyncJobExecutor syncJobExecutor;
    private AdaptiveChunkSizer chunkSizer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        LockAssert.TestHelper.makeAllAssertsPass(true);
        syncJobExecutor = new SyncJobExecutor(checkpointService, metrics);
        chunkSizer = new AdaptiveChunkSizer(500, 10, 1000);
        when(adapter.getJobName()).thenReturn("ORDERS_SYNC");
    }

    @Test
    void execute_정상_흐름_테스트() {
        when(checkpointService.getLastId(anyString())).thenReturn(100L);

        Object record = new Object();
        List<Object> records = Collections.singletonList(record);
        when(adapter.readChunk(eq(100L), anyInt())).thenReturn(records);
        when(adapter.extractLastId(record)).thenReturn(101L);

        syncJobExecutor.execute(adapter, chunkSizer, retryProcessor, 100000L);

        verify(adapter).bulkUpsert(records);
        verify(checkpointService).update(eq("ORDERS_SYNC"), eq(101L), eq(1), anyInt());
        verify(retryProcessor).processQueue();
    }

    @Test
    void execute_데이터_없을때_조기호출_종료_테스트() {
        when(checkpointService.getLastId(anyString())).thenReturn(100L);
        when(adapter.readChunk(anyLong(), anyInt())).thenReturn(Collections.emptyList());

        syncJobExecutor.execute(adapter, chunkSizer, retryProcessor, 100000L);

        verify(adapter, never()).bulkUpsert(anyList());
        verify(checkpointService, never()).update(anyString(), anyLong(), anyInt(), any());
    }

    @Test
    void execute_정상완료후_tickDuration_기록() {
        when(checkpointService.getLastId(anyString())).thenReturn(0L);
        when(adapter.readChunk(anyLong(), anyInt())).thenReturn(Collections.emptyList());

        syncJobExecutor.execute(adapter, chunkSizer, retryProcessor, 100000L);

        verify(metrics).recordTickDuration(anyString(), anyLong());
    }

    @Test
    void execute_예외발생해도_tickDuration_기록_finally보장() {
        when(checkpointService.getLastId(anyString())).thenReturn(0L);
        when(adapter.readChunk(anyLong(), anyInt())).thenThrow(new RuntimeException("강제 오류"));

        syncJobExecutor.execute(adapter, chunkSizer, retryProcessor, 100000L);

        verify(metrics).recordTickDuration(anyString(), anyLong());
    }
}
