package com.feple.feple_backend.auth.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminWriteOperationRateLimiterTest {

    @Test
    void 한도_초과전까지는_true() {
        AdminWriteOperationRateLimiter limiter = new AdminWriteOperationRateLimiter();

        for (int i = 0; i < 120; i++) {
            assertThat(limiter.tryConsume("admin1")).isTrue();
        }
    }

    @Test
    void 한도_초과시_false() {
        AdminWriteOperationRateLimiter limiter = new AdminWriteOperationRateLimiter();
        for (int i = 0; i < 120; i++) {
            limiter.tryConsume("admin1");
        }

        assertThat(limiter.tryConsume("admin1")).isFalse();
    }

    @Test
    void 관리자가_다르면_독립적으로_카운트() {
        AdminWriteOperationRateLimiter limiter = new AdminWriteOperationRateLimiter();
        for (int i = 0; i < 120; i++) {
            limiter.tryConsume("admin1");
        }

        assertThat(limiter.tryConsume("admin2")).isTrue();
    }
}
