package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.DailyStatDto;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.search.repository.SearchLogRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminMetricsServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PostRepository postRepository;
    @Mock CommentRepository commentRepository;
    @Mock PostReportRepository reportRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock ArtistRepository artistRepository;
    @Mock SearchLogRepository searchLogRepository;

    @InjectMocks AdminMetricsServiceImpl adminMetricsService;

    @Test
    void 전체_사용자수_조회시_userRepository_count_위임() {
        given(userRepository.count()).willReturn(42L);

        long result = adminMetricsService.getTotalUserCount();

        assertThat(result).isEqualTo(42L);
        verify(userRepository).count();
    }

    @Test
    void 최근_가입자_5명_조회시_userRepository_위임() {
        List<User> users = List.of(mock(User.class), mock(User.class));
        given(userRepository.findTop5ByDeletedAtIsNullOrderByIdDesc()).willReturn(users);

        List<User> result = adminMetricsService.getRecentUsers();

        assertThat(result).isEqualTo(users);
        verify(userRepository).findTop5ByDeletedAtIsNullOrderByIdDesc();
    }

    @Test
    void 일별통계가_7개_반환됨() {
        given(userRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(0L);
        given(postRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(0L);
        given(commentRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(0L);
        given(reportRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(0L);

        List<DailyStatDto> stats = adminMetricsService.getDailyStats();

        assertThat(stats).hasSize(7);
    }

    @Test
    void 일별통계_첫번째가_6일전_마지막이_오늘() {
        given(userRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);
        given(postRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);
        given(commentRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);
        given(reportRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d");
        String expectedFirst = LocalDate.now().minusDays(6).format(fmt);
        String expectedLast = LocalDate.now().format(fmt);

        List<DailyStatDto> stats = adminMetricsService.getDailyStats();

        assertThat(stats.get(0).date()).isEqualTo(expectedFirst);
        assertThat(stats.get(6).date()).isEqualTo(expectedLast);
    }

    @Test
    void 일별통계_날짜_포맷이_M_d_형식임() {
        given(userRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);
        given(postRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);
        given(commentRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);
        given(reportRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);

        Pattern mdPattern = Pattern.compile("^\\d{1,2}/\\d{1,2}$");

        List<DailyStatDto> stats = adminMetricsService.getDailyStats();

        stats.forEach(s -> assertThat(s.date()).matches(mdPattern));
    }

    @Test
    void 일별통계_각_저장소가_날짜별로_7회_조회됨() {
        given(userRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);
        given(postRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);
        given(commentRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);
        given(reportRepository.countByCreatedAtBetween(any(), any())).willReturn(0L);

        adminMetricsService.getDailyStats();

        verify(userRepository, times(7)).countByCreatedAtBetween(any(), any());
        verify(postRepository, times(7)).countByCreatedAtBetween(any(), any());
        verify(commentRepository, times(7)).countByCreatedAtBetween(any(), any());
        verify(reportRepository, times(7)).countByCreatedAtBetween(any(), any());
    }

    @Test
    void 일별통계_값이_저장소_반환값과_일치함() {
        given(userRepository.countByCreatedAtBetween(any(), any())).willReturn(3L);
        given(postRepository.countByCreatedAtBetween(any(), any())).willReturn(10L);
        given(commentRepository.countByCreatedAtBetween(any(), any())).willReturn(25L);
        given(reportRepository.countByCreatedAtBetween(any(), any())).willReturn(1L);

        List<DailyStatDto> stats = adminMetricsService.getDailyStats();

        stats.forEach(s -> {
            assertThat(s.signups()).isEqualTo(3L);
            assertThat(s.posts()).isEqualTo(10L);
            assertThat(s.comments()).isEqualTo(25L);
            assertThat(s.reports()).isEqualTo(1L);
        });
    }
}
