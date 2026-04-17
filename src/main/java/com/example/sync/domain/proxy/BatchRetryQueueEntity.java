package com.example.sync.domain.proxy;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch_retry_queue")
public class BatchRetryQueueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "source_ids")
    private String sourceIds;

    @Column(name = "error_type", length = 100)
    private String errorType;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "retry_count")
    private int retryCount = 0;

    @Column(name = "max_retry")
    private int maxRetry = 5;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    public BatchRetryQueueEntity() {}

    private BatchRetryQueueEntity(Builder builder) {
        this.id = builder.id;
        this.sourceIds = builder.sourceIds;
        this.errorType = builder.errorType;
        this.errorMessage = builder.errorMessage;
        this.retryCount = builder.retryCount;
        this.maxRetry = builder.maxRetry;
        this.nextRetryAt = builder.nextRetryAt;
        this.status = builder.status;
    }

    public static Builder builder() {
        return new Builder();
    }


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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public static class Builder {
        private Long id;
        private String sourceIds;
        private String errorType;
        private String errorMessage;
        private int retryCount = 0;
        private int maxRetry = 5;
        private LocalDateTime nextRetryAt;
        private String status = "PENDING";

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder sourceIds(String sourceIds) {
            this.sourceIds = sourceIds;
            return this;
        }

        public Builder errorType(String errorType) {
            this.errorType = errorType;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public Builder maxRetry(int maxRetry) {
            this.maxRetry = maxRetry;
            return this;
        }

        public Builder nextRetryAt(LocalDateTime nextRetryAt) {
            this.nextRetryAt = nextRetryAt;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public BatchRetryQueueEntity build() {
            return new BatchRetryQueueEntity(this);
        }
    }
}
