package com.example.sync.service.retry;

public final class RetryStatus {
    public static final String PENDING    = "PENDING";
    public static final String PROCESSING = "PROCESSING";
    public static final String SUCCESS    = "SUCCESS";
    public static final String DEAD       = "DEAD";

    public static boolean isDead(String status) {
        return DEAD.equals(status);
    }

    public static boolean isTerminal(String status) {
        return SUCCESS.equals(status) || isDead(status);
    }

    private RetryStatus() {}
}
