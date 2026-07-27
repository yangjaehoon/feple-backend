package com.feple.feple_backend.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.post.dto.PostAdminFilterDto;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ArtistPostFilterStrategyTest {

    @Mock PostRepository postRepository;

    @InjectMocks ArtistPostFilterStrategy strategy;

    private final PageRequest pageable = PageRequest.of(0, 20);

    @Test
    void filterKey는_ARTIST() {
        assertThat(strategy.filterKey()).isEqualTo("ARTIST");
    }

    @Test
    void 아티스트ID와_키워드_모두있으면_해당_조회() {
        Page<Post> page = new PageImpl<>(java.util.List.of());
        given(postRepository.findByArtistIdAndTitleLikeOrderByCreatedAtDesc(1L, "키워드", pageable)).willReturn(page);

        Page<Post> result = strategy.findPosts(new PostAdminFilterDto(0, 20, null, "키워드", 1L, null), "키워드", pageable);

        assertThat(result).isSameAs(page);
    }

    @Test
    void 아티스트ID만_있으면_전체조회() {
        Page<Post> page = new PageImpl<>(java.util.List.of());
        given(postRepository.findByArtistIdOrderByCreatedAtDesc(1L, pageable)).willReturn(page);

        Page<Post> result = strategy.findPosts(new PostAdminFilterDto(0, 20, null, null, 1L, null), "", pageable);

        assertThat(result).isSameAs(page);
        verify(postRepository, never()).findByArtistIdAndTitleLikeOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    void 아티스트ID없고_키워드있으면_전체_아티스트글_중_키워드검색() {
        Page<Post> page = new PageImpl<>(java.util.List.of());
        given(postRepository.findByArtistIsNotNullAndTitleLikeOrderByCreatedAtDesc("키워드", pageable)).willReturn(page);

        Page<Post> result = strategy.findPosts(new PostAdminFilterDto(0, 20, null, "키워드", null, null), "키워드", pageable);

        assertThat(result).isSameAs(page);
    }

    @Test
    void 아티스트ID_키워드_모두없으면_전체_아티스트글_조회() {
        Page<Post> page = new PageImpl<>(java.util.List.of());
        given(postRepository.findByArtistIsNotNullOrderByCreatedAtDesc(pageable)).willReturn(page);

        Page<Post> result = strategy.findPosts(new PostAdminFilterDto(0, 20, null, null, null, null), "", pageable);

        assertThat(result).isSameAs(page);
    }
}
