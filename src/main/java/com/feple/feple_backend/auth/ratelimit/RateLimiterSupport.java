package com.feple.feple_backend.auth.ratelimit;

import com.feple.feple_backend.global.exception.TooManyRequestsException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import java.time.Duration;

/**
 * 키(IP/사용자ID 등)별로 Caffeine 캐시에 Bucket4j 버킷을 두고 토큰을 소비하는 공통 구현.
 * 각 RateLimiter는 이 클래스에 위임하고, 자신만의 한도/캐시 파라미터만 지정한다.
 */
final class RateLimiterSupport {

    static final String TOO_MANY_REQUESTS_MESSAGE = "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.";

    private final Cache<String, Bucket> cache;
    private final int capacity;
    private final Duration refillPeriod;

    RateLimiterSupport(Duration cacheExpireAfterAccess, long cacheMaximumSize,
                        int capacity, Duration refillPeriod) {
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(cacheExpireAfterAccess)
                .maximumSize(cacheMaximumSize)
                .build();
        this.capacity = capacity;
        this.refillPeriod = refillPeriod;
    }

    boolean tryConsume(String key) {
        return resolveBucket(key).tryConsume(1);
    }

    void checkOrThrow(String key) {
        if (!tryConsume(key)) {
            throw new TooManyRequestsException(TOO_MANY_REQUESTS_MESSAGE);
        }
    }

    private Bucket resolveBucket(String key) {
        return cache.get(key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity, refillPeriod)
                        .build())
                .build());
    }
}
