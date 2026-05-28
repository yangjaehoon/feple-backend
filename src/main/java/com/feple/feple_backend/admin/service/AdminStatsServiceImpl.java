package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.ContentTrendDto;
import com.feple.feple_backend.admin.DailyStatDto;
import com.feple.feple_backend.admin.TopKeywordDto;
import com.feple.feple_backend.admin.UserActivityStatsDto;
import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.entity.SongRequest;
import com.feple.feple_backend.artist.song.entity.SongRequestStatus;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.search.repository.SearchLogRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsServiceImpl implements AdminStatsService {

    private static final DateTimeFormatter STAT_DATE_FORMAT = DateTimeFormatter.ofPattern("M/d");

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostReportRepository reportRepository;
    private final FestivalCertificationRepository certificationRepository;
    private final SongRequestRepository songRequestRepository;
    private final FestivalRepository festivalRepository;
    private final ArtistRepository artistRepository;
    private final SearchLogRepository searchLogRepository;

    @Override
    public long getTotalUserCount() {
        return userRepository.count();
    }

    @Override
    public List<User> getRecentUsers() {
        return userRepository.findTop5ByOrderByIdDesc();
    }

    @Override
    public List<DailyStatDto> getDailyStats() {
        LocalDate today = LocalDate.now();
        return buildDailyStats(today.minusDays(AdminConstants.STATS_RECENT_DAYS - 1), today);
    }

    @Override
    public List<DailyStatDto> getMonthlyStats() {
        LocalDate today = LocalDate.now();
        return buildDailyStats(today.minusDays(AdminConstants.STATS_MONTHLY_DAYS - 1), today);
    }

    @Override
    public List<DailyStatDto> getRangeStats(LocalDate from, LocalDate to) {
        return buildDailyStats(from, to);
    }

    @Override
    public UserActivityStatsDto getUserActivityStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime weekStart = today.minusDays(6).atStartOfDay();
        LocalDateTime monthStart = today.minusDays(29).atStartOfDay();

        return new UserActivityStatsDto(
                nullToZero(userRepository.countActiveUsersBetween(dayStart, dayEnd)),
                nullToZero(userRepository.countActiveUsersBetween(weekStart, dayEnd)),
                nullToZero(userRepository.countActiveUsersBetween(monthStart, dayEnd)),
                userRepository.countByCreatedAtBetween(dayStart, dayEnd),
                userRepository.countByCreatedAtBetween(weekStart, dayEnd),
                userRepository.countByCreatedAtBetween(monthStart, dayEnd)
        );
    }

    private List<DailyStatDto> buildDailyStats(LocalDate from, LocalDate to) {
        List<DailyStatDto> stats = new ArrayList<>();
        LocalDate date = from;
        while (!date.isAfter(to)) {
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            stats.add(new DailyStatDto(
                    date.format(STAT_DATE_FORMAT),
                    userRepository.countByCreatedAtBetween(start, end),
                    postRepository.countByCreatedAtBetween(start, end),
                    commentRepository.countByCreatedAtBetween(start, end),
                    reportRepository.countByCreatedAtBetween(start, end)
            ));
            date = date.plusDays(1);
        }
        return stats;
    }

    @Override
    public ContentTrendDto getContentTrend() {
        LocalDateTime since7days = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime since30days = LocalDate.now().minusDays(29).atStartOfDay();
        LocalDate today = LocalDate.now();

        List<TopKeywordDto> topKeywords = searchLogRepository
                .findTopKeywordsSince(since7days, 10)
                .stream()
                .map(row -> new TopKeywordDto((String) row[0], ((Number) row[1]).longValue()))
                .toList();

        List<Festival> topFestivalsByLike = festivalRepository.findTop10ByOrderByLikeCountDesc();

        List<Festival> upcomingHotFestivals = festivalRepository.findUpcomingFestivalsSortedByLike(
                today, today.plusDays(30), PageRequest.of(0, 5));

        List<Artist> topArtistsByFollower = artistRepository.findTop10ByOrderByFollowerCountDesc();

        List<Post> topPostsByLike = postRepository.findHotPosts(since30days, PageRequest.of(0, 10));

        return new ContentTrendDto(topKeywords, topFestivalsByLike, upcomingHotFestivals,
                topArtistsByFollower, topPostsByLike);
    }

    private static long nullToZero(Long value) {
        return value != null ? value : 0L;
    }

    @Override
    public List<FestivalCertification> getPendingCerts(int limit) {
        return certificationRepository
                .findByStatusOrderByCreatedAtDesc(CertificationStatus.PENDING, PageRequest.of(0, limit))
                .getContent();
    }

    @Override
    public long getPendingCertCount() {
        return certificationRepository.countByStatus(CertificationStatus.PENDING);
    }

    @Override
    public List<PostReport> getPendingReports(int limit) {
        return reportRepository
                .findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, PageRequest.of(0, limit))
                .getContent();
    }

    @Override
    public long getPendingReportCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    @Override
    public List<SongRequest> getPendingSongRequests(int limit) {
        return songRequestRepository.findByStatusOrderByCreatedAtDesc(
                SongRequestStatus.PENDING, PageRequest.of(0, limit));
    }

    @Override
    public long getPendingSongRequestCount() {
        return songRequestRepository.countByStatus(SongRequestStatus.PENDING);
    }
}
