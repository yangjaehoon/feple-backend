package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostScrap;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.repository.PostScrapRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostScrapServiceTest {

    @Mock PostScrapRepository postScrapRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;

    @InjectMocks PostScrapService postScrapService;

    private User user(Long id) {
        return User.builder().id(id).nickname("user" + id)
                .oauthId("o" + id).role(UserRole.USER).build();
    }

    private Post post(Long id, User author) {
        return Post.builder()
                .id(id).title("제목" + id).content("내용")
                .user(author).boardType(BoardType.FREE)
                .likeCount(0).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ── isScrapedByUser ──────────────────────────────────────────────

    @Test
    void userId가_null이면_false_반환_레포지토리_미호출() {
        boolean result = postScrapService.isScrapedByUser(1L, null);

        assertThat(result).isFalse();
        verify(postScrapRepository, never()).existsByUserIdAndPostId(any(), any());
    }

    @Test
    void 스크랩한_게시글이면_true_반환() {
        given(postScrapRepository.existsByUserIdAndPostId(1L, 10L)).willReturn(true);

        assertThat(postScrapService.isScrapedByUser(10L, 1L)).isTrue();
    }

    @Test
    void 스크랩_안_한_게시글이면_false_반환() {
        given(postScrapRepository.existsByUserIdAndPostId(1L, 10L)).willReturn(false);

        assertThat(postScrapService.isScrapedByUser(10L, 1L)).isFalse();
    }

    // ── toggleScrap ──────────────────────────────────────────────────

    @Test
    void 스크랩_취소시_delete가_호출되고_false_반환() {
        User user = user(1L);
        Post post = post(10L, user);
        PostScrap existing = PostScrap.builder().user(user).post(post).build();
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(postScrapRepository.findByUserIdAndPostId(1L, 10L)).willReturn(Optional.of(existing));

        boolean result = postScrapService.toggleScrap(10L, 1L);

        assertThat(result).isFalse();
        verify(postScrapRepository).delete(existing);
        verify(postScrapRepository, never()).save(any(PostScrap.class));
    }

    @Test
    void 스크랩_추가시_save가_호출되고_카운터_증가_true_반환() {
        User user = user(1L);
        Post post = post(10L, user);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(postScrapRepository.findByUserIdAndPostId(1L, 10L)).willReturn(Optional.empty());

        boolean result = postScrapService.toggleScrap(10L, 1L);

        assertThat(result).isTrue();
        assertThat(post.getScrapCount()).isEqualTo(1);
        verify(postScrapRepository).save(any(PostScrap.class));
    }

    @Test
    void 존재하지_않는_게시글에_스크랩시_예외() {
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postScrapService.toggleScrap(99L, 1L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 존재하지_않는_사용자가_스크랩시_예외() {
        User user = user(1L);
        Post post = post(10L, user);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postScrapService.toggleScrap(10L, 99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── getMyScraps ──────────────────────────────────────────────────

    @Test
    void 스크랩_목록_조회시_게시글_DTO_목록_반환() {
        User user = user(1L);
        Post post1 = post(10L, user);
        Post post2 = post(11L, user);
        PostScrap scrap1 = PostScrap.builder().user(user).post(post1).build();
        PostScrap scrap2 = PostScrap.builder().user(user).post(post2).build();
        given(postScrapRepository.findByUserIdOrderByIdDesc(1L)).willReturn(List.of(scrap1, scrap2));

        List<PostResponseDto> result = postScrapService.getMyScraps(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(1).getId()).isEqualTo(11L);
    }

    @Test
    void 스크랩_없으면_빈_목록_반환() {
        given(postScrapRepository.findByUserIdOrderByIdDesc(1L)).willReturn(List.of());

        assertThat(postScrapService.getMyScraps(1L)).isEmpty();
    }
}
