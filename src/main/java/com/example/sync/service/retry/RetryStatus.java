package com.example.sync.service.retry;

public enum RetryStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    DEAD;

    public boolean isDead() {
        return this == DEAD;
    }

    public boolean isTerminal() {
        return this == SUCCESS || this == DEAD;
    }

}
