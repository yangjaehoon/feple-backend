package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.dto.ReportSubmitRequest;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.service.ReportAdminService;
import com.feple.feple_backend.global.QueryResultMapper;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.ReportRejectionService;
import com.feple.feple_backend.global.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import com.feple.feple_backend.global.cache.EvictAdminReportCaches;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReportService implements ReportAdminService<PostReport> {

    private final PostReportRepository reportRepository;
    private final PostRepository postRepository;
    private final PostAdminService postAdminService;
    private final UserRepository userRepository;

    @Transactional
    public void submitReport(Long postId, Long reporterId, ReportSubmitRequest command) {
        if (reportRepository.existsByReporterIdAndPostId(reporterId, postId)) {
            throw new ConflictException("이미 신고한 게시글입니다.");
        }
        Post post = EntityLoader.getOrThrow(postRepository::findById, postId, "게시글");
        User reporter = EntityLoader.getOrThrow(userRepository::findById, reporterId, "사용자");

        reportRepository.save(PostReport.builder()
                .post(post)
                .reporter(reporter)
                .reason(command.reason())
                .detail(command.detail())
                .build());
    }

    @Cacheable(value = "adminReportTypeCounts", key = "'postPending'")
    public long getPendingCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    @Cacheable(value = "adminReportTypeCounts", key = "'postTotal'")
    public long getTotalCount() {
        return reportRepository.count();
    }

    @Override
    public String getReportType() { return "post"; }

    @Override
    public Page<PostReport> findPendingReports(PageRequest pageable) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable);
    }

    @Override
    public Page<PostReport> findAllReports(PageRequest pageable) {
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public Page<PostReport> searchReportsByKeyword(String keyword, ReportStatus status, PageRequest pageable) {
        return reportRepository.searchByKeyword(keyword, status, pageable);
    }

    @Override
    @EvictAdminReportCaches
    @Transactional
    public void deleteContentAndResolve(Long reportId) {
        PostReport report = EntityLoader.getOrThrow(reportRepository::findById, reportId, "신고");
        Long postId = report.getPostId();
        postAdminService.deletePost(postId);
        reportRepository.findByPostId(postId)
                .forEach(r -> r.resolve(ReportStatus.POST_DELETED));
    }

    @EvictAdminReportCaches
    @Transactional
    public void dismissReport(Long reportId) {
        ReportRejectionService.dismiss(reportRepository, reportId);
    }

    @Override
    @EvictAdminReportCaches
    @Transactional
    public void bulkDismiss(List<Long> ids) {
        ReportRejectionService.bulkDismiss(reportRepository, ids);
    }

    @Override
    public Long extractAuthorId(PostReport report) { return report.getPostAuthorId(); }

    @Override
    public Map<Long, Long> getAuthorReportCounts(Collection<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return QueryResultMapper.toLongMap(reportRepository.countByPostAuthorIds(userIds));
    }

    public List<PostReport> getAllPostReportsForExport() {
        return reportRepository.findAllForExport(PageRequest.of(0, AdminConstants.MAX_EXPORT_ROWS));
    }

    public long getReportCountForUser(Long userId) {
        return QueryResultMapper.extractSingleCount(reportRepository.countByPostAuthorIds(List.of(userId)));
    }
}
