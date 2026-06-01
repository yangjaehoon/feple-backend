package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostLike;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @Mock PostLikeRepository postLikeRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;

    @InjectMocks PostLikeService postLikeService;

    private User user(Long id) {
        return User.builder().id(id).nickname("user" + id)
                .oauthId("o" + id).role(UserRole.USER).build();
    }

    private Post post(Long id, User author) {
        return Post.builder()
                .id(id).title("제목").content("내용")
                .user(author).boardType(BoardType.FREE)
                .likeCount(0).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ── isLikedByUser ────────────────────────────────────────────────

    @Test
    void userId가_null이면_false_반환_레포지토리_미호출() {
        boolean result = postLikeService.isLikedByUser(1L, null);

        assertThat(result).isFalse();
        verify(postLikeRepository, never()).existsByUserIdAndPostId(any(), any());
    }

    @Test
    void 좋아요_한_게시글이면_true_반환() {
        given(postLikeRepository.existsByUserIdAndPostId(1L, 10L)).willReturn(true);

        assertThat(postLikeService.isLikedByUser(10L, 1L)).isTrue();
    }

    @Test
    void 좋아요_안_한_게시글이면_false_반환() {
        given(postLikeRepository.existsByUserIdAndPostId(1L, 10L)).willReturn(false);

        assertThat(postLikeService.isLikedByUser(10L, 1L)).isFalse();
    }

    // ── toggleLike ───────────────────────────────────────────────────

    @Test
    void 좋아요_취소시_decrementLikeCount_호출되고_false_반환() {
        User user = user(1L);
        Post post = post(10L, user);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(postLikeRepository.deleteByUserIdAndPostId(1L, 10L)).willReturn(1);

        boolean result = postLikeService.toggleLike(10L, 1L);

        assertThat(result).isFalse();
        verify(postRepository).decrementLikeCount(10L);
        verify(postLikeRepository, never()).save(any(PostLike.class));
    }

    @Test
    void 좋아요_추가시_save와_incrementLikeCount_호출되고_true_반환() {
        User user = user(1L);
        Post post = post(10L, user);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(postLikeRepository.deleteByUserIdAndPostId(1L, 10L)).willReturn(0);

        boolean result = postLikeService.toggleLike(10L, 1L);

        assertThat(result).isTrue();
        verify(postLikeRepository).save(any(PostLike.class));
        verify(postRepository).incrementLikeCount(10L);
    }

    @Test
    void 동시_좋아요_요청_두_스레드_모두_예외_없이_완료() throws InterruptedException {
        // 두 스레드가 동시에 toggleLike 호출 — 서비스 레이어에서는 예외 없이 완료
        // 실제 중복 삽입 방지는 PostLike(user_id, post_id) DB unique 제약이 담당
        User user = user(1L);
        Post post = post(10L, user);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(postLikeRepository.deleteByUserIdAndPostId(1L, 10L)).willReturn(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 2; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    postLikeService.toggleLike(10L, 1L);
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        assertThat(doneLatch.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(errors).isEmpty();
    }

    @Test
    void 존재하지_않는_게시글에_좋아요시_예외() {
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postLikeService.toggleLike(99L, 1L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 존재하지_않는_사용자가_좋아요시_예외() {
        User user = user(1L);
        Post post = post(10L, user);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postLikeService.toggleLike(10L, 99L))
                .isInstanceOf(NoSuchElementException.class);
    }
}
