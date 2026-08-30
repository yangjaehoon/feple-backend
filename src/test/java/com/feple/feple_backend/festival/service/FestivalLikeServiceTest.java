package com.feple.feple_backend.festival.service;

import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalLike;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class FestivalLikeServiceTest {

    @Mock FestivalLikeRepository festivalLikeRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock UserRepository userRepository;

    @InjectMocks FestivalLikeService festivalLikeService;

    private Festival festival(Long id) {
        return Festival.builder().id(id).title("페스티벌" + id).build();
    }

    private Festival festivalWithLikeCount(Long id, int likeCount) {
        return Festival.builder().id(id).title("페스티벌" + id).likeCount(likeCount).build();
    }

    // ── isLiked ──────────────────────────────────────────────────────

    @Test
    void 찜한_페스티벌이면_true_반환() {
        given(festivalLikeRepository.existsByUserIdAndFestivalId(1L, 5L)).willReturn(true);

        assertThat(festivalLikeService.isLiked(5L, 1L)).isTrue();
    }

    @Test
    void 찜_안_한_페스티벌이면_false_반환() {
        given(festivalLikeRepository.existsByUserIdAndFestivalId(1L, 5L)).willReturn(false);

        assertThat(festivalLikeService.isLiked(5L, 1L)).isFalse();
    }

    // ── toggleLike ───────────────────────────────────────────────────

    @Test
    void 찜_취소시_좋아요수_감소되고_false_반환() {
        User user = user(1L);
        Festival festival = festivalWithLikeCount(5L, 1);
        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(festivalLikeRepository.existsByUserIdAndFestivalId(1L, 5L)).willReturn(true);
        given(festivalLikeRepository.deleteByUserIdAndFestivalId(1L, 5L)).willReturn(1);

        boolean result = festivalLikeService.toggleLike(5L, 1L);

        assertThat(result).isFalse();
        verify(festivalRepository).decrementLikeCount(5L);
        verify(festivalLikeRepository, never()).saveAndFlush(any(FestivalLike.class));
    }

    @Test
    void 찜_취소_요청이지만_동시_삭제로_0건이면_좋아요수_감소_안함() {
        User user = user(1L);
        Festival festival = festival(5L);
        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(festivalLikeRepository.existsByUserIdAndFestivalId(1L, 5L)).willReturn(true);
        given(festivalLikeRepository.deleteByUserIdAndFestivalId(1L, 5L)).willReturn(0);

        boolean result = festivalLikeService.toggleLike(5L, 1L);

        assertThat(result).isFalse();
        verify(festivalRepository, never()).decrementLikeCount(5L);
    }

    @Test
    void 찜_추가시_save_호출되고_좋아요수_증가되며_true_반환() {
        User user = user(1L);
        Festival festival = festival(5L);
        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(festivalLikeRepository.existsByUserIdAndFestivalId(1L, 5L)).willReturn(false);

        boolean result = festivalLikeService.toggleLike(5L, 1L);

        assertThat(result).isTrue();
        verify(festivalLikeRepository).saveAndFlush(any(FestivalLike.class));
        verify(festivalRepository).incrementLikeCount(5L);
        verify(festivalLikeRepository, never()).deleteByUserIdAndFestivalId(1L, 5L);
    }

    @Test
    void 찜_추가_중_동시_저장으로_unique_위반나면_예외없이_true_반환하고_증가_안함() {
        User user = user(1L);
        Festival festival = festival(5L);
        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(festivalLikeRepository.existsByUserIdAndFestivalId(1L, 5L)).willReturn(false);
        given(festivalLikeRepository.saveAndFlush(any(FestivalLike.class)))
                .willThrow(new DataIntegrityViolationException("unique violation"));

        boolean result = festivalLikeService.toggleLike(5L, 1L);

        assertThat(result).isTrue();
        verify(festivalRepository, never()).incrementLikeCount(5L);
    }

    @Test
    void 존재하지_않는_페스티벌에_찜시_예외() {
        given(festivalRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> festivalLikeService.toggleLike(99L, 1L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 존재하지_않는_사용자가_찜시_예외() {
        Festival festival = festival(5L);
        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> festivalLikeService.toggleLike(5L, 99L))
                .isInstanceOf(NoSuchElementException.class);
    }
}
