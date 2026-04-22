package com.example.sync.service.retry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RetryStatusTest {

    @Test
    void isDead() {
        RetryStatus dead = RetryStatus.DEAD;
        assertTrue(dead.isDead());
    }

    @Test
    void isTerminal() {
        RetryStatus success = RetryStatus.SUCCESS;
        assertTrue(success.isTerminal());

        RetryStatus dead = RetryStatus.DEAD;
        assertTrue(dead.isTerminal());
    }
}