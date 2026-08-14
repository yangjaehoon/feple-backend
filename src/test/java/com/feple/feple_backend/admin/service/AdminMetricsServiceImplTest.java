package com.feple.feple_backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.admin.dashboard.ContentTrendDto;
import com.feple.feple_backend.admin.dashboard.DailyStatDto;
import com.feple.feple_backend.admin.dashboard.UserActivityStatsDto;
import com.feple.feple_backend.admin.dashboard.UserSummaryDto;
import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.search.repository.SearchLogRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminMetricsServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PostRepository postRepository;
    @Mock CommentRepository commentRepository;
    @Mock PostReportRepository reportRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock ArtistRepository artistRepository;
    @Mock SearchLogRepository searchLogRepository;

    @Mock FileStorageService fileStorageService;
    @InjectMocks AdminMetricsServiceImpl adminMetricsService;

    private void stubEmptyStats() {
        given(userRepository.countGroupByDate(any(), any())).willReturn(List.of());
        given(postRepository.countGroupByDate(any(), any())).willReturn(List.of());
        given(commentRepository.countPerDate(any(), any())).willReturn(List.of());
        given(reportRepository.countGroupByDate(any(), any())).willReturn(List.of());
    }

    private List<Object[]> rowsForAllDays(long value) {
        LocalDate today = LocalDate.now();
        List<Object[]> rows = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            rows.add(new Object[]{ Date.valueOf(today.minusDays(i)), value });
        }
        return rows;
    }

    @Test
    void 전체_사용자수_조회시_탈퇴회원_제외하고_반환() {
        given(userRepository.countByDeletedAtIsNull()).willReturn(42L);

        long result = adminMetricsService.getTotalUserCount();

        assertThat(result).isEqualTo(42L);
        verify(userRepository).countByDeletedAtIsNull();
    }

    @Test
    void 최근_가입자_5명_조회시_userRepository_위임() {
        List<User> users = List.of(mock(User.class), mock(User.class));
        given(userRepository.findTop5ByDeletedAtIsNullOrderByIdDesc()).willReturn(users);

        List<UserSummaryDto> result = adminMetricsService.getRecentUsers();

        assertThat(result).hasSize(2);
        verify(userRepository).findTop5ByDeletedAtIsNullOrderByIdDesc();
    }

    @Test
    void 일별통계가_7개_반환됨() {
        stubEmptyStats();

        assertThat(adminMetricsService.getDailyStats()).hasSize(7);
    }

    @Test
    void 일별통계_첫번째가_6일전_마지막이_오늘() {
        stubEmptyStats();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("M/d");
        String expectedFirst = LocalDate.now().minusDays(6).format(fmt);
        String expectedLast = LocalDate.now().format(fmt);

        List<DailyStatDto> stats = adminMetricsService.getDailyStats();

        assertThat(stats.get(0).date()).isEqualTo(expectedFirst);
        assertThat(stats.get(6).date()).isEqualTo(expectedLast);
    }

    @Test
    void 일별통계_날짜_포맷이_M_d_형식임() {
        stubEmptyStats();

        Pattern mdPattern = Pattern.compile("^\\d{1,2}/\\d{1,2}$");

        adminMetricsService.getDailyStats()
                .forEach(s -> assertThat(s.date()).matches(mdPattern));
    }

    @Test
    void 일별통계_각_저장소가_1회_일괄_조회됨() {
        stubEmptyStats();

        adminMetricsService.getDailyStats();

        verify(userRepository).countGroupByDate(any(), any());
        verify(postRepository).countGroupByDate(any(), any());
        verify(commentRepository).countPerDate(any(), any());
        verify(reportRepository).countGroupByDate(any(), any());
    }

    @Test
    void 일별통계_값이_저장소_반환값과_일치함() {
        given(userRepository.countGroupByDate(any(), any())).willReturn(rowsForAllDays(3L));
        given(postRepository.countGroupByDate(any(), any())).willReturn(rowsForAllDays(10L));
        given(commentRepository.countPerDate(any(), any())).willReturn(rowsForAllDays(25L));
        given(reportRepository.countGroupByDate(any(), any())).willReturn(rowsForAllDays(1L));

        List<DailyStatDto> stats = adminMetricsService.getDailyStats();

        stats.forEach(s -> {
            assertThat(s.signups()).isEqualTo(3L);
            assertThat(s.posts()).isEqualTo(10L);
            assertThat(s.comments()).isEqualTo(25L);
            assertThat(s.reports()).isEqualTo(1L);
        });
    }

    @Test
    void 기간별_통계는_from부터_to까지의_일수만큼_반환() {
        stubEmptyStats();
        LocalDate from = LocalDate.now().minusDays(2);
        LocalDate to = LocalDate.now();

        List<DailyStatDto> stats = adminMetricsService.getRangeStats(from, to);

        assertThat(stats).hasSize(3);
    }

    @Test
    void 사용자_활동_통계는_activeUsers가_null이면_0으로_대체() {
        given(userRepository.countActiveUsersBetween(any(), any())).willReturn(null);
        given(userRepository.countByCreatedAtBetween(any(), any())).willReturn(5L);

        UserActivityStatsDto result = adminMetricsService.getUserActivityStats();

        assertThat(result.dau()).isEqualTo(0L);
        assertThat(result.wau()).isEqualTo(0L);
        assertThat(result.mau()).isEqualTo(0L);
        assertThat(result.signupsToday()).isEqualTo(5L);
    }

    @Test
    void 사용자_활동_통계는_저장소_반환값을_그대로_반영() {
        given(userRepository.countActiveUsersBetween(any(), any())).willReturn(9L);
        given(userRepository.countByCreatedAtBetween(any(), any())).willReturn(3L);

        UserActivityStatsDto result = adminMetricsService.getUserActivityStats();

        assertThat(result.dau()).isEqualTo(9L);
        assertThat(result.wau()).isEqualTo(9L);
        assertThat(result.mau()).isEqualTo(9L);
        assertThat(result.signupsToday()).isEqualTo(3L);
        assertThat(result.signupsThisWeek()).isEqualTo(3L);
        assertThat(result.signupsThisMonth()).isEqualTo(3L);
    }

    @Test
    void 콘텐츠_트렌드는_각_저장소_결과를_조합해_반환() {
        given(searchLogRepository.findTopKeywordsSince(any(LocalDateTime.class), anyInt()))
                .willReturn(List.<Object[]>of(new Object[]{"페스티벌", 10L}));
        Festival festival = mock(Festival.class);
        given(festivalRepository.findTop10ByDeletedAtIsNullOrderByLikeCountDesc()).willReturn(List.of(festival));
        given(festivalRepository.findUpcomingFestivalsSortedByLike(any(), any(), any())).willReturn(List.of(festival));
        Artist artist = mock(Artist.class);
        given(artistRepository.findTop10ByDeletedAtIsNullOrderByFollowerCountDesc()).willReturn(List.of(artist));
        Post post = mock(Post.class);
        given(postRepository.findPopularPosts(any(), any())).willReturn(List.of(post));

        ContentTrendDto result = adminMetricsService.getContentTrend();

        assertThat(result.topKeywords()).hasSize(1);
        assertThat(result.topKeywords().get(0).keyword()).isEqualTo("페스티벌");
        assertThat(result.topFestivalsByLike()).containsExactly(festival);
        assertThat(result.upcomingHotFestivals()).containsExactly(festival);
        assertThat(result.topArtistsByFollower()).containsExactly(artist);
        assertThat(result.topPostsByLike()).containsExactly(post);
    }
}
