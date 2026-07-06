package com.feple.feple_backend.post.service;

import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 인기글 조회 결과를 캐싱하는 전용 컴포넌트.
 * PostServiceImpl 내부에서 self-invocation으로 호출하면 @Cacheable 프록시가 우회되므로
 * 별도 빈으로 분리했다 — 사용자별 차단 필터링은 캐시된 원본 목록을 가져온 뒤 적용한다.
 */
@Component
@RequiredArgsConstructor
class HotPostCache {

    private final PostRepository postRepository;

    @Cacheable("hotPosts")
    List<PostResponseDto> get() {
        return postRepository.findHotPosts(LocalDateTime.now().minusWeeks(1), PageRequest.of(0, PageSize.HOT_POSTS))
                .stream()
                .map(PostResponseDto::from)
                .toList();
    }
}
