package com.example.sync.domain.proxy;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
public class BatchRetryQueue {

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

    @Column(name = "job_name", length = 50)
    private String jobName = "ORDERS_SYNC";

    public BatchRetryQueue() {}

    private BatchRetryQueue(Builder builder) {
        this.id = builder.id;
        this.sourceIds = builder.sourceIds;
        this.errorType = builder.errorType;
        this.errorMessage = builder.errorMessage;
        this.retryCount = builder.retryCount;
        this.maxRetry = builder.maxRetry;
        this.nextRetryAt = builder.nextRetryAt;
        this.status = builder.status;
        this.jobName = builder.jobName;
    }

    public static Builder builder() {
        return new Builder();
    }


    public Long getId() { return id; }

    public String getSourceIds() { return sourceIds; }

    public String getErrorType() { return errorType; }

    public String getErrorMessage() { return errorMessage; }
    public int getRetryCount() { return retryCount; }

    public int getMaxRetry() { return maxRetry; }

    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public static class Builder {
        private Long id;
        private String sourceIds;
        private String errorType;
        private String errorMessage;
        private int retryCount = 0;
        private int maxRetry = 5;
        private LocalDateTime nextRetryAt;
        private String status = "PENDING";
        private String jobName = "ORDERS_SYNC";

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

        public Builder jobName(String jobName) {
            this.jobName = jobName;
            return this;
        }

        public BatchRetryQueue build() {
            return new BatchRetryQueue(this);
        }
    }
}
