package com.feple.feple_backend.auth.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 관리자 계정 기준 POST 요청 제한.
 * 1분 동안 최대 120회 허용 — 정상 업무에 충분하고 자동화 공격은 차단.
 */
@Component
public class AdminWriteOperationRateLimiter {

    private final RateLimiterSupport limiter =
            new RateLimiterSupport(Duration.ofMinutes(5), 1_000, 120, Duration.ofMinutes(1));

    public boolean tryConsume(String username) {
        return limiter.tryConsume(username);
    }
}
