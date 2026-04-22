package com.example.sync.domain.proxy;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class BatchRetryQueueTest {

    @Test
    void builder() {
        BatchRetryQueue entity = BatchRetryQueue.builder()
                .id(1L)
                .sourceIds("test")
                .errorType("test")
                .errorMessage("test")
                .retryCount(1)
                .maxRetry(5)
                .nextRetryAt(LocalDateTime.now())
                .status("PENDING")
                .build();
    }
}