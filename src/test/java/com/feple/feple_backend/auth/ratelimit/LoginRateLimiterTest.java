package com.feple.feple_backend.auth.ratelimit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.feple.feple_backend.global.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    @Test
    void 한도_초과전까지는_통과() {
        LoginRateLimiter limiter = new LoginRateLimiter();

        for (int i = 0; i < 10; i++) {
            assertThatCode(() -> limiter.check("1.2.3.4")).doesNotThrowAnyException();
        }
    }

    @Test
    void 한도_초과시_예외() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < 10; i++) {
            limiter.check("1.2.3.4");
        }

        assertThatThrownBy(() -> limiter.check("1.2.3.4"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("요청이 너무 많습니다");
    }

    @Test
    void IP가_다르면_독립적으로_카운트() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        for (int i = 0; i < 10; i++) {
            limiter.check("1.1.1.1");
        }

        assertThatCode(() -> limiter.check("2.2.2.2")).doesNotThrowAnyException();
    }
}
