package com.feple.feple_backend.post.service;

import static com.feple.feple_backend.support.TestEntityFactory.freePost;
import static com.feple.feple_backend.support.TestEntityFactory.freePostWithLikeCount;
import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostLike;
import com.feple.feple_backend.post.event.PostUnlikedEvent;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @Mock PostLikeRepository postLikeRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks PostLikeService postLikeService;

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
    void 좋아요_취소시_좋아요수_감소되고_false_반환() {
        User user = user(1L);
        Post post = freePostWithLikeCount(10L, user, 1);
        given(postRepository.findVisibleById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(postLikeRepository.deleteByUserIdAndPostId(1L, 10L)).willReturn(1);

        boolean result = postLikeService.toggleLike(10L, 1L);

        assertThat(result).isFalse();
        verify(postRepository).decrementLikeCount(10L);
        verify(postLikeRepository, never()).saveAndFlush(any(PostLike.class));
    }

    @Test
    void 좋아요_추가시_save_호출되고_좋아요수_증가되며_true_반환() {
        User user = user(1L);
        Post post = freePost(10L, user);
        given(postRepository.findVisibleById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(postLikeRepository.deleteByUserIdAndPostId(1L, 10L)).willReturn(0);

        boolean result = postLikeService.toggleLike(10L, 1L);

        assertThat(result).isTrue();
        verify(postLikeRepository).saveAndFlush(any(PostLike.class));
        verify(postRepository).incrementLikeCount(10L);
    }

    @Test
    void 동시요청_경합으로_unique_제약_위반이어도_예외없이_true_반환() {
        User user = user(1L);
        Post post = freePost(10L, user);
        given(postRepository.findVisibleById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(postLikeRepository.deleteByUserIdAndPostId(1L, 10L)).willReturn(0);
        given(postLikeRepository.saveAndFlush(any(PostLike.class)))
                .willThrow(new org.springframework.dao.DataIntegrityViolationException("unique violation"));

        boolean result = postLikeService.toggleLike(10L, 1L);

        assertThat(result).isTrue();
        verify(postRepository, never()).incrementLikeCount(10L);
    }

    @Test
    void 동시_좋아요_요청_두_스레드_모두_예외_없이_완료() throws InterruptedException {
        // 두 스레드가 동시에 toggleLike 호출 — 서비스 레이어에서는 예외 없이 완료
        // 실제 중복 삽입 방지는 PostLike(user_id, post_id) DB unique 제약이 담당
        User user = user(1L);
        Post post = freePost(10L, user);
        given(postRepository.findVisibleById(10L)).willReturn(Optional.of(post));
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
        given(postRepository.findVisibleById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postLikeService.toggleLike(99L, 1L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 존재하지_않는_사용자가_좋아요시_예외() {
        User user = user(1L);
        Post post = freePost(10L, user);
        given(postRepository.findVisibleById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postLikeService.toggleLike(10L, 99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 타인_글_좋아요_취소시_포인트_회수_이벤트를_발행한다() {
        // 지급(PostLikedEvent)만 있고 회수가 없으면 좋아요/취소 반복으로 글쓴이에게
        // 포인트를 무한 적립할 수 있다.
        User author = user(2L);
        User liker = user(1L);
        Post post = freePostWithLikeCount(10L, author, 1);
        given(postRepository.findVisibleById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(liker));
        given(postLikeRepository.deleteByUserIdAndPostId(1L, 10L)).willReturn(1);

        boolean result = postLikeService.toggleLike(10L, 1L);

        assertThat(result).isFalse();
        ArgumentCaptor<PostUnlikedEvent> captor = ArgumentCaptor.forClass(PostUnlikedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().postAuthorId()).isEqualTo(2L);
        assertThat(captor.getValue().postId()).isEqualTo(10L);
        assertThat(captor.getValue().unlikerId()).isEqualTo(1L);
    }

    @Test
    void 자기_글_좋아요_취소는_회수_이벤트를_발행하지_않는다() {
        // 지급 쪽도 자기 글은 건너뛰므로 회수도 대칭으로 건너뛰어야 한다.
        User author = user(1L);
        Post post = freePostWithLikeCount(10L, author, 1);
        given(postRepository.findVisibleById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postLikeRepository.deleteByUserIdAndPostId(1L, 10L)).willReturn(1);

        postLikeService.toggleLike(10L, 1L);

        verify(eventPublisher, never()).publishEvent(any(PostUnlikedEvent.class));
    }
}
