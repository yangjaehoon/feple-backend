package com.feple.feple_backend.auth.ratelimit;

import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * IP 주소 기준으로 인증 시도를 제한한다.
 * 10분 동안 최대 10회 허용, 초과 시 429 응답.
 * Caffeine 캐시로 15분 미접근 시 자동 만료, 최대 10,000개 IP 저장.
 */
@Component
public class LoginRateLimiter {

    private final RateLimiterSupport limiter =
            new RateLimiterSupport(Duration.ofMinutes(15), 10_000, 10, Duration.ofMinutes(10));

    public void check(String ip) {
        limiter.checkOrThrow(ip);
    }
}
