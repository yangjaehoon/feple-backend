package com.feple.feple_backend.auth.ratelimit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.feple.feple_backend.global.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

class WriteOperationRateLimiterTest {

    @Test
    void 한도_초과전까지는_통과() {
        WriteOperationRateLimiter limiter = new WriteOperationRateLimiter();

        for (int i = 0; i < 30; i++) {
            assertThatCode(() -> limiter.check("user:1")).doesNotThrowAnyException();
        }
    }

    @Test
    void 한도_초과시_예외() {
        WriteOperationRateLimiter limiter = new WriteOperationRateLimiter();
        for (int i = 0; i < 30; i++) {
            limiter.check("user:1");
        }

        assertThatThrownBy(() -> limiter.check("user:1"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("요청이 너무 많습니다");
    }

    @Test
    void 키가_다르면_독립적으로_카운트() {
        WriteOperationRateLimiter limiter = new WriteOperationRateLimiter();
        for (int i = 0; i < 30; i++) {
            limiter.check("user:1");
        }

        assertThatCode(() -> limiter.check("user:2")).doesNotThrowAnyException();
    }
}
