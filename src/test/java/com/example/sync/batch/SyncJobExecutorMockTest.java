package com.example.sync.batch;

import com.example.sync.infrastructure.batch.AdaptiveChunkSizer;
import com.example.sync.base.SyncJobExecutor;
import com.example.sync.infrastructure.checkpoint.CheckpointService;
import com.example.sync.infrastructure.monitoring.LagMonitor;
import com.example.sync.infrastructure.monitoring.SyncMetrics;
import com.example.sync.repository.reader.SourceDataReader;
import com.example.sync.dto.SourceRecordDto;
import com.example.sync.infrastructure.retry.RetryQueueProcessor;
import com.example.sync.repository.writer.TargetDataWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SyncJobExecutorMockTest {

    @Mock private CheckpointService checkpointService;
    @Mock private SourceDataReader reader;
    @Mock private TargetDataWriter writer;
    @Mock private AdaptiveChunkSizer chunkSizer;
    @Mock private LagMonitor lagMonitor;
    @Mock private RetryQueueProcessor retryProcessor;
    @Mock private SyncMetrics metrics;

    @InjectMocks
    private SyncJobExecutor syncJobExecutor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void execute_정상_흐름_테스트() {
        // Given
        when(checkpointService.getLastId(anyString())).thenReturn(100L);
        when(chunkSizer.getChunkSize()).thenReturn(500);
        
        SourceRecordDto record = new SourceRecordDto(101L, "ORD-101", 1001L, "CREATED", new BigDecimal("100.00"), LocalDateTime.now());
        List<SourceRecordDto> records = Arrays.asList(record);
        
        when(reader.readChunk(eq(100L), eq(500))).thenReturn(records);

        // When
        syncJobExecutor.execute();

        // Then
        verify(writer, times(1)).bulkUpsert(records);
        verify(checkpointService, times(1)).update(eq("ORDERS_SYNC"), eq(101L), eq(1));
        verify(chunkSizer, times(1)).adjust(anyLong());
        verify(retryProcessor, times(1)).processQueue();
    }

    @Test
    void execute_데이터_없을때_조기호출_종료_테스트() {
        // Given
        when(checkpointService.getLastId(anyString())).thenReturn(100L);
        when(chunkSizer.getChunkSize()).thenReturn(500);
        when(reader.readChunk(anyLong(), anyInt())).thenReturn(Collections.emptyList());

        // When
        syncJobExecutor.execute();

        // Then
        verify(writer, never()).bulkUpsert(anyList());
        verify(checkpointService, never()).update(anyString(), anyLong(), anyInt());
    }
}
