package com.feple.feple_backend.auth.ratelimit;

import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * IP 주소 기준으로 /auth/refresh, /auth/logout 호출을 제한한다.
 * 로그인 브루트포스 방지용 {@link LoginRateLimiter}와 별도 버킷을 쓴다 — 유효한
 * refresh 토큰이 있어야만 성공하는 작업이라 자격증명 추측 대상이 아니고, CGNAT 등
 * 공유 IP에서 여러 유저가 동시에 토큰을 갱신하면 로그인 버킷을 금방 소진시켜
 * 무관한 사용자들이 강제 로그아웃되는 문제가 있었다.
 * 10분 동안 최대 30회 허용, 초과 시 429 응답.
 */
@Component
public class TokenRefreshRateLimiter {

    private final RateLimiterSupport limiter =
            new RateLimiterSupport(Duration.ofMinutes(15), 10_000, 30, Duration.ofMinutes(10));

    public void check(String ip) {
        limiter.checkOrThrow(ip);
    }
}
