package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.repository.PostRepository;

import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static com.feple.feple_backend.support.TestEntityFactory.freePost;
import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PostActivityServiceImplTest {

    @Mock PostRepository postRepository;
    @Mock PostLikeRepository postLikeRepository;
    @Mock UserRepository userRepository;

    @InjectMocks PostActivityServiceImpl postActivityService;

    // ── getMyPosts / getMyPostsPaged ────────────────────────────────────

    @Test
    void 내_게시글_목록_조회() {
        User author = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.findByUserOrderByCreatedAtDesc(eq(author), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(freePost(1L, author))));

        List<PostResponseDto> result = postActivityService.getMyPosts(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void 존재하지_않는_사용자의_내_게시글_조회시_예외() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postActivityService.getMyPosts(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 내_게시글_커서_페이징_조회() {
        User author = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.findByUserOrderByCreatedAtDesc(eq(author), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(freePost(1L, author))));

        CursorPage<PostResponseDto> result = postActivityService.getMyPostsPaged(1L, null, 20);

        assertThat(result.content()).hasSize(1);
    }

    // ── getPublicPostsPaged ──────────────────────────────────────────

    @Test
    void 공개_게시글_커서_페이징_조회() {
        User author = user(1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.findPublicByUserOrderByCreatedAtDesc(eq(author), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(freePost(1L, author))));

        CursorPage<PostResponseDto> result = postActivityService.getPublicPostsPaged(1L, null, 20);

        assertThat(result.content()).hasSize(1);
    }

    // ── countPublicPosts ─────────────────────────────────────────────

    @Test
    void 공개_게시글_수_조회() {
        given(postRepository.countPublicByUserId(1L)).willReturn(4L);

        assertThat(postActivityService.countPublicPosts(1L)).isEqualTo(4L);
    }

    // ── getLikedPosts / countLikedPosts ─────────────────────────────────

    @Test
    void 좋아요한_게시글_목록_조회() {
        User author = user(1L);
        given(postLikeRepository.findPostsByUserId(eq(1L), any(Pageable.class)))
                .willReturn(List.of(freePost(1L, author)));

        List<PostResponseDto> result = postActivityService.getLikedPosts(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void 좋아요한_게시글_수_조회() {
        given(postLikeRepository.countByUserId(1L)).willReturn(2L);

        assertThat(postActivityService.countLikedPosts(1L)).isEqualTo(2L);
    }
}
