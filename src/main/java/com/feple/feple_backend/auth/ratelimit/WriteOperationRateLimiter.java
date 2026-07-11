package com.feple.feple_backend.auth.ratelimit;

import com.feple.feple_backend.global.exception.TooManyRequestsException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 사용자 ID(인증된 경우) 또는 IP 주소(미인증) 기준으로 변경 요청을 제한한다.
 * 1분 동안 최대 30회 허용, 초과 시 429 응답.
 */
@Component
public class WriteOperationRateLimiter {

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .maximumSize(50_000)
            .build();

    private Bucket resolveBucket(String ip) {
        return cache.get(ip, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(30)
                        .refillGreedy(30, Duration.ofMinutes(1))
                        .build())
                .build());
    }

    public void check(String ip) {
        if (!resolveBucket(ip).tryConsume(1)) {
            throw new TooManyRequestsException("요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }
    }
}
