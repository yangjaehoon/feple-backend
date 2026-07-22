package com.feple.feple_backend.festival.service;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalAttendance;
import com.feple.feple_backend.festival.repository.FestivalAttendanceRepository;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.NoSuchElementException;
import java.util.Optional;

import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FestivalAttendanceServiceTest {

    @Mock FestivalAttendanceRepository attendanceRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock UserRepository userRepository;

    @InjectMocks FestivalAttendanceService festivalAttendanceService;

    private Festival festival(Long id) {
        return Festival.builder().id(id).title("페스티벌" + id).build();
    }

    // ── isAttending ──────────────────────────────────────────────────

    @Test
    void 참석중이면_true_반환() {
        given(attendanceRepository.existsByUserIdAndFestivalId(1L, 5L)).willReturn(true);

        assertThat(festivalAttendanceService.isAttending(5L, 1L)).isTrue();
    }

    @Test
    void 참석중이_아니면_false_반환() {
        given(attendanceRepository.existsByUserIdAndFestivalId(1L, 5L)).willReturn(false);

        assertThat(festivalAttendanceService.isAttending(5L, 1L)).isFalse();
    }

    // ── toggleAttending ──────────────────────────────────────────────

    @Test
    void 참석_취소시_참석수_감소되고_false_반환() {
        User user = user(1L);
        Festival festival = festival(5L);
        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(attendanceRepository.deleteByUserIdAndFestivalId(1L, 5L)).willReturn(1);

        boolean result = festivalAttendanceService.toggleAttending(5L, 1L);

        assertThat(result).isFalse();
        verify(festivalRepository).decrementAttendingCount(5L);
        verify(attendanceRepository, never()).saveAndFlush(any(FestivalAttendance.class));
    }

    @Test
    void 참석_추가시_저장되고_참석수_증가되며_true_반환() {
        User user = user(1L);
        Festival festival = festival(5L);
        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(attendanceRepository.deleteByUserIdAndFestivalId(1L, 5L)).willReturn(0);

        boolean result = festivalAttendanceService.toggleAttending(5L, 1L);

        assertThat(result).isTrue();
        verify(attendanceRepository).saveAndFlush(any(FestivalAttendance.class));
        verify(festivalRepository).incrementAttendingCount(5L);
    }

    @Test
    void 동시요청_경합으로_unique_제약_위반이어도_예외없이_true_반환() {
        User user = user(1L);
        Festival festival = festival(5L);
        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(attendanceRepository.deleteByUserIdAndFestivalId(1L, 5L)).willReturn(0);
        given(attendanceRepository.saveAndFlush(any(FestivalAttendance.class)))
                .willThrow(new DataIntegrityViolationException("unique violation"));

        boolean result = festivalAttendanceService.toggleAttending(5L, 1L);

        assertThat(result).isTrue();
        verify(festivalRepository, never()).incrementAttendingCount(5L);
    }

    @Test
    void 존재하지_않는_페스티벌에_참석시_예외() {
        given(festivalRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> festivalAttendanceService.toggleAttending(99L, 1L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 존재하지_않는_사용자가_참석시_예외() {
        Festival festival = festival(5L);
        given(festivalRepository.findById(5L)).willReturn(Optional.of(festival));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> festivalAttendanceService.toggleAttending(5L, 99L))
                .isInstanceOf(NoSuchElementException.class);
    }
}
