package com.feple.feple_backend.post.service;

import static com.feple.feple_backend.support.TestEntityFactory.freePost;
import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PopularPostCacheTest {

    @Mock PostRepository postRepository;

    @InjectMocks PopularPostCache cache;

    @Test
    void 인기글_저장소_결과를_dto로_매핑() {
        User author = user(1L);
        given(postRepository.findPopularPosts(any(LocalDateTime.class), any()))
                .willReturn(List.of(freePost(1L, author), freePost(2L, author)));

        List<PostResponseDto> result = cache.getPopularPosts();

        assertThat(result).hasSize(2);
    }

    @Test
    void 인기글_저장소_조회시_노출개수보다_넉넉한_풀을_요청() {
        // 최종 노출 개수(4)만 캐싱하면, 조회자별 차단 필터링 이후 노출 개수가 줄어들 수 있다 —
        // 캐시는 항상 POPULAR_POSTS_POOL만큼 넉넉히 가져와야 한다
        given(postRepository.findPopularPosts(any(LocalDateTime.class), any()))
                .willReturn(List.of());

        cache.getPopularPosts();

        then(postRepository).should().findPopularPosts(any(LocalDateTime.class),
                argThat((Pageable p) -> p.getPageSize() == PageSize.POPULAR_POSTS_POOL));
    }
}
