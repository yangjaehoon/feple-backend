package com.feple.feple_backend.auth.ratelimit;

import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * 사용자 ID(인증된 경우) 또는 IP 주소(미인증) 기준으로 변경 요청을 제한한다.
 * 1분 동안 최대 30회 허용, 초과 시 429 응답.
 */
@Component
public class WriteOperationRateLimiter {

    private final RateLimiterSupport limiter =
            new RateLimiterSupport(Duration.ofMinutes(5), 50_000, 30, Duration.ofMinutes(1));

    public void check(String ip) {
        limiter.checkOrThrow(ip);
    }
}
