package com.example.sync.infrastructure.exception;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class OracleExceptionClassifierTest {

    private static RuntimeException classify(int oraCode) {
        SQLException sqlEx = new SQLException("ORA-" + oraCode + " test", "99999", oraCode);
        RuntimeException wrapper = new RuntimeException("wrap", sqlEx);
        return OracleExceptionClassifier.classify(wrapper);
    }

    // --- Permanent 코드 ---

    @Test
    void ORA_00001_unique_constraint_Permanent() {
        assertThat(classify(1)).isInstanceOf(PermanentSyncException.class);
    }

    @Test
    void ORA_01400_not_null_Permanent() {
        assertThat(classify(1400)).isInstanceOf(PermanentSyncException.class);
    }

    @Test
    void ORA_01438_precision_Permanent() {
        assertThat(classify(1438)).isInstanceOf(PermanentSyncException.class);
    }

    @Test
    void ORA_02291_fk_parent_Permanent() {
        assertThat(classify(2291)).isInstanceOf(PermanentSyncException.class);
    }

    @Test
    void ORA_02292_fk_child_Permanent() {
        assertThat(classify(2292)).isInstanceOf(PermanentSyncException.class);
    }

    @Test
    void ORA_12899_column_too_large_Permanent() {
        assertThat(classify(12899)).isInstanceOf(PermanentSyncException.class);
    }

    // --- Transient 코드 ---

    @Test
    void ORA_17002_connection_reset_Transient() {
        assertThat(classify(17002)).isInstanceOf(TransientSyncException.class);
    }

    @Test
    void ORA_00060_deadlock_Transient() {
        assertThat(classify(60)).isInstanceOf(TransientSyncException.class);
    }

    @Test
    void ORA_03113_eof_Transient() {
        assertThat(classify(3113)).isInstanceOf(TransientSyncException.class);
    }

    @Test
    void ORA_01034_not_available_Transient() {
        assertThat(classify(1034)).isInstanceOf(TransientSyncException.class);
    }

    // --- 미분류 — 보수적으로 Transient ---

    @Test
    void unknown_code_보수적_Transient() {
        assertThat(classify(99999)).isInstanceOf(TransientSyncException.class);
    }

    @Test
    void no_sql_exception_보수적_Transient() {
        RuntimeException ex = new RuntimeException("알 수 없는 오류");
        assertThat(OracleExceptionClassifier.classify(ex)).isInstanceOf(TransientSyncException.class);
    }
}
