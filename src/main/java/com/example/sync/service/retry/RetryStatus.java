package com.example.sync.service.retry;

public final class RetryStatus {
    public static final String PENDING    = "PENDING";
    public static final String PROCESSING = "PROCESSING";
    public static final String SUCCESS    = "SUCCESS";
    public static final String FAILED     = "FAILED";
    public static final String DEAD       = "DEAD";
    /** 롤링 배포 호환: 구버전이 insert한 'DLQ' 행을 읽기에서만 허용. 쓰기는 항상 DEAD. */
    public static final String LEGACY_DLQ = "DLQ";

    public static boolean isDead(String status) {
        return DEAD.equals(status) || LEGACY_DLQ.equals(status);
    }

    public static boolean isTerminal(String status) {
        return SUCCESS.equals(status) || isDead(status);
    }

    private RetryStatus() {}
}
