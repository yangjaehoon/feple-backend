package com.feple.feple_backend.auth.ratelimit;

import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * IP 주소 기준으로 로그인(토큰 교환) 시도를 제한한다.
 * 10분 동안 최대 60회 허용, 초과 시 429 응답.
 * Caffeine 캐시로 15분 미접근 시 자동 만료, 최대 10,000개 IP 저장.
 *
 * <p>로그인은 카카오 액세스 토큰·Firebase ID 토큰 교환이라 비밀번호처럼 추측할 대상이
 * 없다 — 이 제한의 목적은 무차별 대입 방지가 아니라 외부 API·DB 호출 남용 차단이다.
 * 반면 통신사 CGNAT는 많은 사용자를 한 공인 IP로 묶어, 페스티벌 현장처럼 사람이 몰리면
 * 한도가 낮을수록 정상 사용자가 막힌다. 그래서 남용만 걸러낼 만큼 넉넉하게 둔다.
 */
@Component
public class LoginRateLimiter {

    static final int MAX_ATTEMPTS_PER_WINDOW = 60;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final RateLimiterSupport limiter =
            new RateLimiterSupport(Duration.ofMinutes(15), 10_000, MAX_ATTEMPTS_PER_WINDOW, WINDOW);

    public void check(String ip) {
        limiter.checkOrThrow(ip);
    }
}
