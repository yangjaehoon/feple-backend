package com.feple.feple_backend.auth.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimiterSupportTest {

    @Test
    void 용량만큼_소비후_초과요청은_거부() {
        RateLimiterSupport limiter = new RateLimiterSupport(Duration.ofMinutes(5), 100, 2, Duration.ofMinutes(10));

        assertThat(limiter.tryConsume("k")).isTrue();
        assertThat(limiter.tryConsume("k")).isTrue();
        assertThat(limiter.tryConsume("k")).isFalse();
    }

    @Test
    void 키가_다르면_독립적으로_소비() {
        RateLimiterSupport limiter = new RateLimiterSupport(Duration.ofMinutes(5), 100, 1, Duration.ofMinutes(10));

        assertThat(limiter.tryConsume("a")).isTrue();
        assertThat(limiter.tryConsume("b")).isTrue();
        assertThat(limiter.tryConsume("a")).isFalse();
    }
}
