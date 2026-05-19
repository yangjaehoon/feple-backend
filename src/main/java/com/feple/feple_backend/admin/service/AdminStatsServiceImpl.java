package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.DailyStatDto;
import com.feple.feple_backend.artist.song.entity.SongRequest;
import com.feple.feple_backend.artist.song.entity.SongRequestStatus;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
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

    private static final int STATS_DAYS = 7;
    private static final DateTimeFormatter STAT_DATE_FORMAT = DateTimeFormatter.ofPattern("M/d");

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostReportRepository reportRepository;
    private final FestivalCertificationRepository certificationRepository;
    private final SongRequestRepository songRequestRepository;

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
        List<DailyStatDto> stats = new ArrayList<>();
        for (int i = STATS_DAYS - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            stats.add(new DailyStatDto(
                date.format(STAT_DATE_FORMAT),
                userRepository.countByCreatedAtBetween(start, end),
                postRepository.countByCreatedAtBetween(start, end),
                commentRepository.countByCreatedAtBetween(start, end),
                reportRepository.countByCreatedAtBetween(start, end)
            ));
        }
        return stats;
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
