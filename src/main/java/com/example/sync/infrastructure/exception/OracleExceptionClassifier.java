package com.example.sync.infrastructure.exception;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/**
 * Oracle JDBC 오류를 재시도 가능(Transient)과 재시도 불가(Permanent)로 분류.
 * SQLException의 ErrorCode (ORA-xxxxx) 기반 판정.
 */
public final class OracleExceptionClassifier {

    private static final Set<Integer> PERMANENT_CODES = new HashSet<>();
    private static final Set<Integer> TRANSIENT_CODES = new HashSet<>();

    static {
        // 데이터/스키마 오류 — 재시도 무의미
        PERMANENT_CODES.add(1);      // ORA-00001 unique constraint violation
        PERMANENT_CODES.add(1400);   // ORA-01400 NOT NULL
        PERMANENT_CODES.add(1438);   // ORA-01438 value larger than specified precision
        PERMANENT_CODES.add(1722);   // ORA-01722 invalid number
        PERMANENT_CODES.add(1840);   // ORA-01840 input value not long enough for date
        PERMANENT_CODES.add(2291);   // ORA-02291 FK parent key not found
        PERMANENT_CODES.add(2292);   // ORA-02292 FK child record found
        PERMANENT_CODES.add(12899);  // ORA-12899 value too large for column

        // 네트워크/가용성 — 재시도 가치 있음
        TRANSIENT_CODES.add(17002);  // IO Error: connection reset
        TRANSIENT_CODES.add(17008);  // Closed Connection
        TRANSIENT_CODES.add(17410);  // No more data to read from socket
        TRANSIENT_CODES.add(1033);   // ORA-01033 ORACLE initialization or shutdown in progress
        TRANSIENT_CODES.add(1034);   // ORA-01034 ORACLE not available
        TRANSIENT_CODES.add(1089);   // ORA-01089 immediate shutdown in progress
        TRANSIENT_CODES.add(3113);   // ORA-03113 end-of-file on communication channel
        TRANSIENT_CODES.add(3114);   // ORA-03114 not connected to ORACLE
        TRANSIENT_CODES.add(12170);  // ORA-12170 TNS:Connect timeout
        TRANSIENT_CODES.add(12541);  // ORA-12541 TNS:no listener
        TRANSIENT_CODES.add(60);     // ORA-00060 deadlock
    }

    private OracleExceptionClassifier() {}

    /**
     * 원인 예외 체인을 탐색하여 Permanent/Transient로 분류.
     * 둘 중 어느 것도 명확히 판정되지 않으면 보수적으로 Transient로 처리한다.
     */
    public static RuntimeException classify(Throwable t) {
        SQLException sqlEx = findSqlException(t);
        if (sqlEx != null) {
            int code = sqlEx.getErrorCode();
            if (PERMANENT_CODES.contains(code)) {
                return new PermanentSyncException(
                    "ORA-" + code + ": " + sqlEx.getMessage(), t);
            }
            if (TRANSIENT_CODES.contains(code)) {
                return new TransientSyncException(
                    "ORA-" + code + ": " + sqlEx.getMessage(), t);
            }
        }
        if (findCause(t, IOException.class) != null) {
            return new TransientSyncException("IO error during sync", t);
        }
        // 미분류 — 보수적으로 Transient 처리
        return new TransientSyncException("Unclassified error: " + t.getMessage(), t);
    }

    private static SQLException findSqlException(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof SQLException) return (SQLException) cur;
            cur = cur.getCause();
        }
        return null;
    }

    private static <T extends Throwable> T findCause(Throwable t, Class<T> type) {
        Throwable cur = t;
        while (cur != null) {
            if (type.isInstance(cur)) return type.cast(cur);
            cur = cur.getCause();
        }
        return null;
    }
}
