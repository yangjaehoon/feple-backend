package com.feple.feple_backend.post.service;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.OwnershipValidator;
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
    private final FileStorageService fileStorageService;

    /**
     * 차단 필터링용 실제 작성자 id(authorUserId)와, 클라이언트에 노출할 익명화된 DTO를 함께 담는다.
     * PostResponseDto.userId는 익명 글이면 null이라 캐시 이후 시점에는 차단 여부를 판단할 수 없다 —
     * DTO 생성 시점(이 메서드 안)에만 알 수 있는 실제 작성자 id를 별도로 들고 있어야 한다.
     */
    record Entry(Long authorUserId, PostResponseDto dto) {
        /** 캐시된 dto는 익명 글이면 조회자와 무관하게 userId를 비워둔다 — 본인 글이면 여기서 복원한다. */
        PostResponseDto dtoFor(Long viewerId) {
            if (dto.getUserId() != null || !OwnershipValidator.isOwner(authorUserId, viewerId)) return dto;
            return dto.toBuilder().userId(authorUserId).build();
        }
    }

    // 최종 노출 개수(POPULAR_POSTS)가 아니라 넉넉한 풀(POPULAR_POSTS_POOL)을 캐싱한다 —
    // 조회자별 차단 필터링은 이 캐시 이후에 적용되므로, 딱 4개만 캐싱하면 그중 일부가
    // 차단 작성자일 때 노출 개수가 눈에 띄게 줄어든다.
    @Cacheable("popularPosts")
    List<Entry> getPopularPosts() {
        return postRepository.findPopularPosts(LocalDateTime.now().minusWeeks(1), PageRequest.of(0, PageSize.POPULAR_POSTS_POOL))
                .stream()
                .map(post -> new Entry(post.getUserId(), PostResponseDto.from(post, fileStorageService)))
                .toList();
    }
}
