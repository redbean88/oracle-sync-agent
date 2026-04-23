package com.example.sync.infrastructure.config;

public class RetryConfig {

    private int maxRetry = 3;
    private int initialBackoffSec = 60;
    private int backoffStepSec = 300;
    private int claimBatchSize = 50;

    public RetryConfig() {}

    public RetryConfig(int maxRetry, int initialBackoffSec, int backoffStepSec, int claimBatchSize) {
        this.maxRetry = maxRetry;
        this.initialBackoffSec = initialBackoffSec;
        this.backoffStepSec = backoffStepSec;
        this.claimBatchSize = claimBatchSize;
    }

    public int getMaxRetry() { return maxRetry; }
    public void setMaxRetry(int maxRetry) { this.maxRetry = maxRetry; }
    public int getInitialBackoffSec() { return initialBackoffSec; }
    public void setInitialBackoffSec(int initialBackoffSec) { this.initialBackoffSec = initialBackoffSec; }
    public int getBackoffStepSec() { return backoffStepSec; }
    public void setBackoffStepSec(int backoffStepSec) { this.backoffStepSec = backoffStepSec; }
    public int getClaimBatchSize() { return claimBatchSize; }
    public void setClaimBatchSize(int claimBatchSize) { this.claimBatchSize = claimBatchSize; }
}
