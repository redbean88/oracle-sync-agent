package com.example.sync.infrastructure.exception;

// 재시도 O — 일시적 오류
public class TransientSyncException extends RuntimeException {
    public TransientSyncException(String message) {
        super(message);
    }

    public TransientSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
