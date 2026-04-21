package com.example.sync.service.retry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetryStatusTest {

    @Test
    void isDead_DEAD_true() {
        assertThat(RetryStatus.isDead(RetryStatus.DEAD)).isTrue();
    }

    @Test
    void isDead_PENDING_false() {
        assertThat(RetryStatus.isDead(RetryStatus.PENDING)).isFalse();
    }

    @Test
    void isDead_SUCCESS_false() {
        assertThat(RetryStatus.isDead(RetryStatus.SUCCESS)).isFalse();
    }

    @Test
    void isTerminal_SUCCESS_true() {
        assertThat(RetryStatus.isTerminal(RetryStatus.SUCCESS)).isTrue();
    }

    @Test
    void isTerminal_DEAD_true() {
        assertThat(RetryStatus.isTerminal(RetryStatus.DEAD)).isTrue();
    }

    @Test
    void isTerminal_PENDING_false() {
        assertThat(RetryStatus.isTerminal(RetryStatus.PENDING)).isFalse();
    }

    @Test
    void isTerminal_PROCESSING_false() {
        assertThat(RetryStatus.isTerminal(RetryStatus.PROCESSING)).isFalse();
    }
}
