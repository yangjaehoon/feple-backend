package com.feple.feple_backend.auth.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * IP 주소 기준으로 인증 시도를 제한한다.
 * 10분 동안 최대 10회 허용, 초과 시 429 응답.
 * Caffeine 캐시로 15분 미접근 시 자동 만료, 최대 10,000개 IP 저장.
 */
@Component
public class LoginRateLimiter {

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    private Bucket resolveBucket(String ip) {
        return cache.get(ip, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillGreedy(10, Duration.ofMinutes(10))
                        .build())
                .build());
    }

    public void check(String ip) {
        if (!resolveBucket(ip).tryConsume(1)) {
            throw new TooManyRequestsException("요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
