package com.feple.feple_backend.post.service;

import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.repository.PostRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * 인기글 조회 결과를 캐싱하는 전용 컴포넌트.
 * PostServiceImpl 내부에서 self-invocation으로 호출하면 @Cacheable 프록시가 우회되므로
 * 별도 빈으로 분리했다 — 사용자별 차단 필터링은 캐시된 원본 목록을 가져온 뒤 적용한다.
 */
@Component
@RequiredArgsConstructor
class PopularPostCache {

    private final PostRepository postRepository;

    // 최종 노출 개수(POPULAR_POSTS)가 아니라 넉넉한 풀(POPULAR_POSTS_POOL)을 캐싱한다 —
    // 조회자별 차단 필터링은 이 캐시 이후에 적용되므로, 딱 4개만 캐싱하면 그중 일부가
    // 차단 작성자일 때 노출 개수가 눈에 띄게 줄어든다.
    @Cacheable("popularPosts")
    List<PostResponseDto> getPopularPosts() {
        return postRepository.findPopularPosts(LocalDateTime.now().minusWeeks(1), PageRequest.of(0, PageSize.POPULAR_POSTS_POOL))
                .stream()
                .map(PostResponseDto::from)
                .toList();
    }
}
