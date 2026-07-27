package com.feple.feple_backend.post.service;

import static com.feple.feple_backend.support.TestEntityFactory.freePost;
import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

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
}
