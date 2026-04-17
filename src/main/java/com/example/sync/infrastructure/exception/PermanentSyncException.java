package com.example.sync.infrastructure.exception;

// 재시도 X — 영구적 오류
public class PermanentSyncException extends RuntimeException {
    public PermanentSyncException(String message) {
        super(message);
    }

    public PermanentSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
