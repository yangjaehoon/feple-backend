package com.feple.feple_backend.auth.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 관리자 계정 기준 POST 요청 제한.
 * 1분 동안 최대 120회 허용 — 정상 업무에 충분하고 자동화 공격은 차단.
 */
@Component
public class AdminWriteOperationRateLimiter {

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .maximumSize(1_000)
            .build();

    private Bucket resolveBucket(String username) {
        return cache.get(username, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(120)
                        .refillGreedy(120, Duration.ofMinutes(1))
                        .build())
                .build());
    }

    public boolean tryConsume(String username) {
        return resolveBucket(username).tryConsume(1);
    }
}
