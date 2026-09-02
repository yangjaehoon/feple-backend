package com.feple.feple_backend.auth.ratelimit;

import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * 게시글 작성 전용 한도 — 일반 쓰기 한도({@link WriteOperationRateLimiter}, 분당 30회)와는
 * 별개의 버킷을 사용자 ID 기준으로 둔다. 10분에 10개까지 허용하며, 좋아요·댓글 등 다른
 * 쓰기 요청과 토큰을 공유하지 않아 정상 활동에는 영향이 없다. 익명 게시글 도배(스팸) 대응.
 */
@Component
public class PostCreationRateLimiter {

    private final RateLimiterSupport limiter =
            new RateLimiterSupport(Duration.ofMinutes(30), 100_000, 10, Duration.ofMinutes(10));

    public void check(Long userId) {
        limiter.checkOrThrow("post-create:user:" + userId);
    }
}
