package com.feple.feple_backend.auth.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP 주소 기준으로 로그인 시도를 제한한다.
 * 10분 동안 최대 10회 허용, 초과 시 429 응답.
 */
@Component
public class LoginRateLimiter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket resolveBucket(String ip) {
        return buckets.computeIfAbsent(ip, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillGreedy(10, Duration.ofMinutes(10))
                        .build())
                .build());
    }

    /**
     * 로그인 시도 가능 여부 확인. 초과 시 TooManyRequestsException 발생.
     */
    public void check(String ip) {
        Bucket bucket = resolveBucket(ip);
        if (!bucket.tryConsume(1)) {
            throw new TooManyRequestsException("로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
