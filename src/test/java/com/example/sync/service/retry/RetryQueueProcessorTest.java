package com.example.sync.service.retry;

import com.example.sync.config.SyncJobProperties;
import com.example.sync.service.adapter.SyncJobAdapter;
import com.example.sync.service.monitoring.SyncMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetryQueueProcessorTest {

    @Mock private SyncJobAdapter<Object> adapter;
    @Mock private RetryQueueRepository repo;
    @Mock private DeadLetterHandler deadLetterHandler;
    @Mock private SyncMetrics metrics;

    private RetryQueueProcessor<Object> processor;

    @BeforeEach
    void setUp() {
        SyncJobProperties.RetryConfig config = new SyncJobProperties.RetryConfig();
        // 기본값: maxRetry=3, initialBackoffSec=60, backoffStepSec=300, claimBatchSize=50
        processor = new RetryQueueProcessor<>(adapter, repo, deadLetterHandler, metrics, config);
        when(adapter.getJobName()).thenReturn("ORDERS_SYNC");
    }

    // ─────────────────────────────────────────────────────
    // enqueue / enqueueDead
    // ─────────────────────────────────────────────────────

    @Test
    void enqueue_backoffSec60으로_save() {
        when(adapter.extractLastId(any())).thenReturn(1L);
        processor.enqueue(Collections.singletonList(new Object()), new RuntimeException("일시적 오류"));

        ArgumentCaptor<Integer> backoffCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(repo).save(any(BatchRetryQueue.class), backoffCaptor.capture());
        assertThat(backoffCaptor.getValue()).isEqualTo(60);
    }

    @Test
    void enqueueDead_backoffSec0으로_save_status_DEAD() {
        when(adapter.extractLastId(any())).thenReturn(1L);
        processor.enqueueDead(Collections.singletonList(new Object()), new RuntimeException("영구 오류"));

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
    void processQueue_claimed_있으면_markSuccess_호출() {
        BatchRetryQueue item = buildItem(1L, 0, 3);
        when(repo.claimPending(anyString(), anyInt())).thenReturn(Collections.singletonList(item));
        when(adapter.fetchByIds(anyList())).thenReturn(Collections.singletonList(new Object()));

        processor.processQueue();

        verify(repo).markSuccess(1L);
    }

    @Test
    void processQueue_claimEmpty_pending있으면_claimZeroMetric() {
        when(repo.claimPending(anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(repo.countPending(anyString())).thenReturn(5L);

        processor.processQueue();

        verify(metrics).incrementRetryClaimZero(anyString());
    }

    @Test
    void processQueue_claimEmpty_pending없으면_metric_미호출() {
        when(repo.claimPending(anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(repo.countPending(anyString())).thenReturn(0L);

        processor.processQueue();

        verify(metrics, never()).incrementRetryClaimZero(anyString());
    }

    // ─────────────────────────────────────────────────────
    // processOneItem
    // ─────────────────────────────────────────────────────

    @Test
    void processOneItem_성공_markSuccess() {
        BatchRetryQueue item = buildItem(1L, 0, 3);
        when(adapter.fetchByIds(anyList())).thenReturn(Collections.singletonList(new Object()));

        processor.processOneItem(item);

        verify(repo).markSuccess(1L);
        verify(deadLetterHandler, never()).handle(any());
    }

    @Test
    void processOneItem_원본없으면_deadLetterHandler_호출() {
        BatchRetryQueue item = buildItem(2L, 0, 3);
        when(adapter.fetchByIds(anyList())).thenReturn(Collections.emptyList());

        processor.processOneItem(item);

        verify(deadLetterHandler).handle(item);
        verify(repo, never()).markSuccess(anyLong());
    }

    @Test
    void processOneItem_첫재시도_backoffSec300() {
        BatchRetryQueue item = buildItem(3L, 0, 3);
        when(adapter.fetchByIds(anyList())).thenThrow(new RuntimeException("DB 오류"));

        processor.processOneItem(item);

        verify(repo).incrementRetry(eq(3L), eq(300));  // backoffStepSec(300) * 1
        verify(deadLetterHandler, never()).handle(any());
    }

    @Test
    void processOneItem_두번째재시도_backoffSec600() {
        BatchRetryQueue item = buildItem(4L, 1, 3);
        when(adapter.fetchByIds(anyList())).thenThrow(new RuntimeException("DB 오류"));

        processor.processOneItem(item);

        verify(repo).incrementRetry(eq(4L), eq(600));  // backoffStepSec(300) * 2
    }

    @Test
    void processOneItem_최종실패_deadLetterHandler_호출() {
        BatchRetryQueue item = buildItem(5L, 3, 3);
        when(adapter.fetchByIds(anyList())).thenThrow(new RuntimeException("반복 오류"));

        processor.processOneItem(item);

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
                .jobName("ORDERS_SYNC")
                .build();
    }
}
