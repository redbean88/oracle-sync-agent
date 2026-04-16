package com.example.sync.retry;

import java.time.LocalDateTime;

public class BatchRetryQueue {
    private Long id;
    private String sourceIds;
    private String errorType;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetry;
    private LocalDateTime nextRetryAt;
    private String status;

    public BatchRetryQueue() {}

    public BatchRetryQueue(Long id, String sourceIds, String errorType, String errorMessage, Integer retryCount, Integer maxRetry, LocalDateTime nextRetryAt, String status) {
        this.id = id;
        this.sourceIds = sourceIds;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.maxRetry = maxRetry;
        this.nextRetryAt = nextRetryAt;
        this.status = status;
    }

    // Getters
    public Long getId() { return id; }
    public String getSourceIds() { return sourceIds; }
    public String getErrorType() { return errorType; }
    public String getErrorMessage() { return errorMessage; }
    public Integer getRetryCount() { return retryCount; }
    public Integer getMaxRetry() { return maxRetry; }
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public String getStatus() { return status; }

    // Manual Builder
    public static BatchRetryQueueBuilder builder() {
        return new BatchRetryQueueBuilder();
    }

    public static class BatchRetryQueueBuilder {
        private Long id;
        private String sourceIds;
        private String errorType;
        private String errorMessage;
        private Integer retryCount;
        private Integer maxRetry;
        private LocalDateTime nextRetryAt;
        private String status;

        public BatchRetryQueueBuilder id(Long id) { this.id = id; return this; }
        public BatchRetryQueueBuilder sourceIds(String sourceIds) { this.sourceIds = sourceIds; return this; }
        public BatchRetryQueueBuilder errorType(String errorType) { this.errorType = errorType; return this; }
        public BatchRetryQueueBuilder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public BatchRetryQueueBuilder retryCount(Integer retryCount) { this.retryCount = retryCount; return this; }
        public BatchRetryQueueBuilder maxRetry(Integer maxRetry) { this.maxRetry = maxRetry; return this; }
        public BatchRetryQueueBuilder nextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; return this; }
        public BatchRetryQueueBuilder status(String status) { this.status = status; return this; }

        public BatchRetryQueue build() {
            return new BatchRetryQueue(id, sourceIds, errorType, errorMessage, retryCount, maxRetry, nextRetryAt, status);
        }
    }
}
