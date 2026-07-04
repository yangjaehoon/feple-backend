package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.repository.PostRepository;

import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.feple.feple_backend.support.TestEntityFactory.freePost;
import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PostSearchServiceImplTest {

    @Mock PostRepository postRepository;

    @InjectMocks PostSearchServiceImpl postSearchService;

    @Test
    void 게시판타입_지정하여_검색() {
        User author = user(1L);
        given(postRepository.findByBoardTypeAndTitleContainingIgnoreCaseOrderByCreatedAtDesc(
                eq(BoardType.FREE), anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(freePost(1L, author))));

        List<PostResponseDto> result = postSearchService.searchPosts("제목", "FREE");

        assertThat(result).hasSize(1);
    }

    @Test
    void 게시판타입_없이_전체_검색() {
        User author = user(1L);
        given(postRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(anyString(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(freePost(1L, author))));

        List<PostResponseDto> result = postSearchService.searchPosts("제목", null);

        assertThat(result).hasSize(1);
    }
}
