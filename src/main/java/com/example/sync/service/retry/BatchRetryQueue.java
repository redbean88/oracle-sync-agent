package com.example.sync.service.retry;

import java.time.LocalDateTime;

/** service 레이어 모델. JPA 엔티티가 아닌 순수 DTO. */
public class BatchRetryQueue {

    private Long id;
    private String sourceIds;
    private String errorType;
    private String errorMessage;
    private int retryCount;
    private int maxRetry;
    private LocalDateTime nextRetryAt;
    private RetryStatus status;
    private String jobName;

    public BatchRetryQueue() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSourceIds() { return sourceIds; }
    public void setSourceIds(String sourceIds) { this.sourceIds = sourceIds; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public int getMaxRetry() { return maxRetry; }
    public void setMaxRetry(int maxRetry) { this.maxRetry = maxRetry; }
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public RetryStatus getStatus() { return status; }
    public void setStatus(RetryStatus status) { this.status = status; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public static BatchRetryQueueBuilder builder() {
        return new BatchRetryQueueBuilder();
    }

    public static class BatchRetryQueueBuilder {
        private Long id;
        private String sourceIds;
        private String errorType;
        private String errorMessage;
        private int retryCount;
        private int maxRetry;
        private LocalDateTime nextRetryAt;
        private RetryStatus status;
        private String jobName;

        public BatchRetryQueueBuilder id(Long id) { this.id = id; return this; }
        public BatchRetryQueueBuilder sourceIds(String sourceIds) { this.sourceIds = sourceIds; return this; }
        public BatchRetryQueueBuilder errorType(String errorType) { this.errorType = errorType; return this; }
        public BatchRetryQueueBuilder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public BatchRetryQueueBuilder retryCount(int retryCount) { this.retryCount = retryCount; return this; }
        public BatchRetryQueueBuilder maxRetry(int maxRetry) { this.maxRetry = maxRetry; return this; }
        public BatchRetryQueueBuilder nextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; return this; }
        public BatchRetryQueueBuilder status(RetryStatus status) { this.status = status; return this; }
        public BatchRetryQueueBuilder jobName(String jobName) { this.jobName = jobName; return this; }

        public BatchRetryQueue build() {
            BatchRetryQueue item = new BatchRetryQueue();
            item.setId(id);
            item.setSourceIds(sourceIds);
            item.setErrorType(errorType);
            item.setErrorMessage(errorMessage);
            item.setRetryCount(retryCount);
            item.setMaxRetry(maxRetry);
            item.setNextRetryAt(nextRetryAt);
            item.setStatus(status);
            item.setJobName(jobName);
            return item;
        }
    }
}
