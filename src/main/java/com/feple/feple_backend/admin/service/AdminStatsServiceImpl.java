package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.DailyStatDto;
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
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsServiceImpl implements AdminStatsService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostReportRepository reportRepository;
    private final FestivalCertificationRepository certificationRepository;

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
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            stats.add(new DailyStatDto(
                date.getMonthValue() + "/" + date.getDayOfMonth(),
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
}
