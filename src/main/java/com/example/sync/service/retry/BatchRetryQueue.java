package com.example.sync.service.retry;

import com.example.sync.service.retry.RetryStatus;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch_retry_queue")
public class BatchRetryQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_ids", length = 1000)
    private String sourceIds;

    @Column(name = "error_type")
    private String errorType;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "retry_count")
    private int retryCount;

    @Column(name = "max_retry")
    private int maxRetry;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "status")
    private RetryStatus status;

    // Constructors
    public BatchRetryQueue() {}

    public BatchRetryQueue(Long id, String sourceIds, String errorType, String errorMessage, int retryCount, int maxRetry, LocalDateTime nextRetryAt, RetryStatus status) {
        this.id = id;
        this.sourceIds = sourceIds;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.maxRetry = maxRetry;
        this.nextRetryAt = nextRetryAt;
        this.status = status;
    }

    // Getters and Setters
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

    // Manual Builder
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

        public BatchRetryQueueBuilder id(Long id) { this.id = id; return this; }
        public BatchRetryQueueBuilder sourceIds(String sourceIds) { this.sourceIds = sourceIds; return this; }
        public BatchRetryQueueBuilder errorType(String errorType) { this.errorType = errorType; return this; }
        public BatchRetryQueueBuilder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public BatchRetryQueueBuilder retryCount(int retryCount) { this.retryCount = retryCount; return this; }
        public BatchRetryQueueBuilder maxRetry(int maxRetry) { this.maxRetry = maxRetry; return this; }
        public BatchRetryQueueBuilder nextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; return this; }
        public BatchRetryQueueBuilder status(RetryStatus status) { this.status = status; return this; }

        public BatchRetryQueue build() {
            BatchRetryQueue entity = new BatchRetryQueue();
            entity.setId(id);
            entity.setSourceIds(sourceIds);
            entity.setErrorType(errorType);
            entity.setErrorMessage(errorMessage);
            entity.setRetryCount(retryCount);
            entity.setMaxRetry(maxRetry);
            entity.setNextRetryAt(nextRetryAt);
            entity.setStatus(status);
            return entity;
        }
    }
}
