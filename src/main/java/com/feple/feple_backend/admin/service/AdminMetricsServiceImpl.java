package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.AdminConstants;
import org.springframework.cache.annotation.Cacheable;
import com.feple.feple_backend.admin.ContentTrendDto;
import com.feple.feple_backend.admin.DailyStatDto;
import com.feple.feple_backend.admin.TopKeywordDto;
import com.feple.feple_backend.admin.UserActivityStatsDto;
import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.search.repository.SearchLogRepository;
import com.feple.feple_backend.admin.UserSummaryDto;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMetricsServiceImpl implements AdminMetricsService {

    private static final DateTimeFormatter STAT_DATE_FORMAT = DateTimeFormatter.ofPattern("M/d");

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostReportRepository reportRepository;
    private final FestivalRepository festivalRepository;
    private final ArtistRepository artistRepository;
    private final SearchLogRepository searchLogRepository;

    @Override
    @Cacheable(value = "adminDashboardStats", key = "'totalUsers'")
    public long getTotalUserCount() {
        return userRepository.countByDeletedAtIsNull();
    }

    @Override
    @Cacheable(value = "adminDashboardStats", key = "'recentUsers'")
    public List<UserSummaryDto> getRecentUsers() {
        return userRepository.findTop5ByDeletedAtIsNullOrderByIdDesc()
                .stream().map(UserSummaryDto::from).toList();
    }

    @Override
    @Cacheable(value = "adminDashboardStats", key = "'dailyStats'")
    public List<DailyStatDto> getDailyStats() {
        LocalDate today = LocalDate.now();
        return buildDailyStats(today.minusDays(AdminConstants.STATS_RECENT_DAYS - 1), today);
    }

    @Override
    @Cacheable(value = "adminRangeStats", key = "#from.toString() + '_' + #to.toString()")
    public List<DailyStatDto> getRangeStats(LocalDate from, LocalDate to) {
        return buildDailyStats(from, to);
    }

    @Override
    @Cacheable("adminActivityStats")
    public UserActivityStatsDto getUserActivityStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime weekStart = today.minusDays(6).atStartOfDay();
        LocalDateTime monthStart = today.minusDays(29).atStartOfDay();

        return new UserActivityStatsDto(
                Objects.requireNonNullElse(userRepository.countActiveUsersBetween(dayStart, dayEnd), 0L),
                Objects.requireNonNullElse(userRepository.countActiveUsersBetween(weekStart, dayEnd), 0L),
                Objects.requireNonNullElse(userRepository.countActiveUsersBetween(monthStart, dayEnd), 0L),
                userRepository.countByCreatedAtBetween(dayStart, dayEnd),
                userRepository.countByCreatedAtBetween(weekStart, dayEnd),
                userRepository.countByCreatedAtBetween(monthStart, dayEnd)
        );
    }

    @Override
    @Cacheable("adminContentTrend")
    public ContentTrendDto getContentTrend() {
        LocalDateTime since7days = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime since30days = LocalDate.now().minusDays(29).atStartOfDay();
        LocalDate today = LocalDate.now();

        List<TopKeywordDto> topKeywords = mapTopKeywords(
                searchLogRepository.findTopKeywordsSince(since7days, 10));

        List<Festival> topFestivalsByLike = festivalRepository.findTop10ByOrderByLikeCountDesc();

        List<Festival> upcomingHotFestivals = festivalRepository.findUpcomingFestivalsSortedByLike(
                today, today.plusDays(30), PageRequest.of(0, 5));

        List<Artist> topArtistsByFollower = artistRepository.findTop10ByOrderByFollowerCountDesc();

        List<Post> topPostsByLike = postRepository.findHotPosts(since30days, PageRequest.of(0, 10));

        return new ContentTrendDto(topKeywords, topFestivalsByLike, upcomingHotFestivals,
                topArtistsByFollower, topPostsByLike);
    }

    private List<DailyStatDto> buildDailyStats(LocalDate from, LocalDate to) {
        LocalDateTime dtFrom = from.atStartOfDay();
        LocalDateTime dtTo = to.plusDays(1).atStartOfDay();

        Map<LocalDate, Long> userMap    = toDateMap(userRepository.countGroupByDate(dtFrom, dtTo));
        Map<LocalDate, Long> postMap    = toDateMap(postRepository.countGroupByDate(dtFrom, dtTo));
        Map<LocalDate, Long> commentMap = toDateMap(commentRepository.countGroupByDate(dtFrom, dtTo));
        Map<LocalDate, Long> reportMap  = toDateMap(reportRepository.countGroupByDate(dtFrom, dtTo));

        List<DailyStatDto> stats = new ArrayList<>();
        LocalDate date = from;
        while (!date.isAfter(to)) {
            stats.add(new DailyStatDto(
                    date.format(STAT_DATE_FORMAT),
                    userMap.getOrDefault(date, 0L),
                    postMap.getOrDefault(date, 0L),
                    commentMap.getOrDefault(date, 0L),
                    reportMap.getOrDefault(date, 0L)
            ));
            date = date.plusDays(1);
        }
        return stats;
    }

    private static List<TopKeywordDto> mapTopKeywords(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new TopKeywordDto((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    private static Map<LocalDate, Long> toDateMap(List<Object[]> rows) {
        Map<LocalDate, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            LocalDate date = row[0] instanceof Date d ? d.toLocalDate() : LocalDate.parse(row[0].toString());
            map.put(date, row[1] != null ? ((Number) row[1]).longValue() : 0L);
        }
        return map;
    }
}
