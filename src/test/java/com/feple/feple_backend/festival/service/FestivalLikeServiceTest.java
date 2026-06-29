package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalLike;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;

import static com.feple.feple_backend.support.TestEntityFactory.user;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
        given(festivalLikeRepository.deleteByUserIdAndFestivalId(1L, 5L)).willReturn(1);

        boolean result = festivalLikeService.toggleLike(5L, 1L);

        assertThat(result).isFalse();
        assertThat(festival.getLikeCount()).isEqualTo(0);
        verify(festivalLikeRepository, never()).save(any(FestivalLike.class));
    }

    @Test
    void 찜_추가시_save_호출되고_좋아요수_증가되며_true_반환() {
        User user = user(1L);
        Festival festival = festival(5L);
        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(festivalLikeRepository.deleteByUserIdAndFestivalId(1L, 5L)).willReturn(0);

        boolean result = festivalLikeService.toggleLike(5L, 1L);

        assertThat(result).isTrue();
        verify(festivalLikeRepository).save(any(FestivalLike.class));
        assertThat(festival.getLikeCount()).isEqualTo(1);
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
