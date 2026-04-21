package com.example.sync.service.retry;

import com.example.sync.domain.source.dto.SourceRecordDto;
import com.example.sync.service.monitoring.SyncMetrics;
import com.example.sync.service.reader.SourceDataReader;
import com.example.sync.service.writer.TargetDataWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryQueueProcessorTest {

    @Mock private RetryQueueRepository repo;
    @Mock private SourceDataReader sourceDataReader;
    @Mock private TargetDataWriter targetDataWriter;
    @Mock private DeadLetterHandler deadLetterHandler;
    @Mock private SyncMetrics metrics;

    @InjectMocks
    private RetryQueueProcessor processor;

    private static List<SourceRecordDto> sampleRecords() {
        return Arrays.asList(
            new SourceRecordDto(1L, "ORD-1", 100L, "CREATED", new BigDecimal("100"), LocalDateTime.now())
        );
    }

    // ─────────────────────────────────────────────────────
    // enqueue / enqueueDead
    // ─────────────────────────────────────────────────────

    @Test
    void enqueue_backoffSec60으로_save() {
        processor.enqueue(sampleRecords(), new RuntimeException("일시적 오류"));

        ArgumentCaptor<Integer> backoffCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(repo).save(any(BatchRetryQueue.class), backoffCaptor.capture());
        assertThat(backoffCaptor.getValue()).isEqualTo(60);
    }

    @Test
    void enqueueDead_backoffSec0으로_save_status_DEAD() {
        processor.enqueueDead(sampleRecords(), new RuntimeException("영구 오류"));

        ArgumentCaptor<BatchRetryQueue> entryCaptor = ArgumentCaptor.forClass(BatchRetryQueue.class);
        ArgumentCaptor<Integer> backoffCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(repo).save(entryCaptor.capture(), backoffCaptor.capture());

        assertThat(backoffCaptor.getValue()).isEqualTo(0);
        assertThat(entryCaptor.getValue().getStatus()).isEqualTo(RetryStatus.DEAD);
    }

    // ─────────────────────────────────────────────────────
    // processQueue
    // ─────────────────────────────────────────────────────

    @Test
    void processQueue_claimed_있으면_forEach_호출() {
        BatchRetryQueue item = buildItem(1L, 0, 3);
        when(repo.claimPending(anyInt())).thenReturn(Collections.singletonList(item));
        when(sourceDataReader.fetchByIds(anyList())).thenReturn(sampleRecords());

        processor.processQueue();

        verify(repo).markSuccess(1L);
    }

    @Test
    void processQueue_claimEmpty_pending있으면_claimZeroMetric() {
        when(repo.claimPending(anyInt())).thenReturn(Collections.emptyList());
        when(repo.countPending()).thenReturn(5L);

        processor.processQueue();

        verify(metrics).incrementRetryClaimZero();
    }

    @Test
    void processQueue_claimEmpty_pending없으면_metric_미호출() {
        when(repo.claimPending(anyInt())).thenReturn(Collections.emptyList());
        when(repo.countPending()).thenReturn(0L);

        processor.processQueue();

        verify(metrics, never()).incrementRetryClaimZero();
    }

    // ─────────────────────────────────────────────────────
    // processOneItem
    // ─────────────────────────────────────────────────────

    @Test
    void processOneItem_성공_markSuccess() {
        BatchRetryQueue item = buildItem(1L, 0, 3);
        when(sourceDataReader.fetchByIds(anyList())).thenReturn(sampleRecords());

        processor.processOneItem(item);

        verify(repo).markSuccess(1L);
        verify(repo, never()).markDead(anyLong());
    }

    @Test
    void processOneItem_원본없으면_markDead() {
        BatchRetryQueue item = buildItem(2L, 0, 3);
        when(sourceDataReader.fetchByIds(anyList())).thenReturn(Collections.emptyList());

        processor.processOneItem(item);

        verify(repo).markDead(2L);
        verify(deadLetterHandler).handle(item);
    }

    @Test
    void processOneItem_첫재시도_backoffSec300() {
        BatchRetryQueue item = buildItem(3L, 0, 3);  // retryCount=0, maxRetry=3
        when(sourceDataReader.fetchByIds(anyList())).thenThrow(new RuntimeException("DB 오류"));

        processor.processOneItem(item);

        verify(repo).incrementRetry(eq(3L), eq(300));  // BACKOFF_STEP_SEC(300) * 1
        verify(repo, never()).markDead(anyLong());
    }

    @Test
    void processOneItem_두번째재시도_backoffSec600() {
        BatchRetryQueue item = buildItem(4L, 1, 3);  // retryCount=1, maxRetry=3
        when(sourceDataReader.fetchByIds(anyList())).thenThrow(new RuntimeException("DB 오류"));

        processor.processOneItem(item);

        verify(repo).incrementRetry(eq(4L), eq(600));  // BACKOFF_STEP_SEC(300) * 2
    }

    @Test
    void processOneItem_최종실패_markDead_DLH호출() {
        BatchRetryQueue item = buildItem(5L, 3, 3);  // retryCount=3, maxRetry=3
        when(sourceDataReader.fetchByIds(anyList())).thenThrow(new RuntimeException("반복 오류"));

        processor.processOneItem(item);

        verify(repo).markDead(5L);
        verify(deadLetterHandler).handle(item);
        verify(repo, never()).incrementRetry(anyLong(), anyInt());
    }

    private static BatchRetryQueue buildItem(Long id, int retryCount, int maxRetry) {
        return BatchRetryQueue.builder()
            .id(id)
            .sourceIds("1")
            .retryCount(retryCount)
            .maxRetry(maxRetry)
            .status(RetryStatus.PROCESSING)
            .build();
    }
}
