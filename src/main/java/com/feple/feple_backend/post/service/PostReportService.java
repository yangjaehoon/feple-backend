package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.admin.service.ReportAdminService;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReportService implements ReportAdminService {

    private final PostReportRepository reportRepository;
    private final PostRepository postRepository;
    private final PostAdminService postAdminService;
    private final UserRepository userRepository;

    @Transactional
    public void submitReport(Long postId, Long reporterId, ReportReason reason, String detail) {
        if (reportRepository.existsByReporterIdAndPostId(reporterId, postId)) {
            throw new ConflictException("이미 신고한 게시글입니다.");
        }
        Post post = EntityFinder.getOrThrow(postRepository::findById, postId, "게시글");
        User reporter = EntityFinder.getOrThrow(userRepository::findById, reporterId, "사용자");

        reportRepository.save(PostReport.builder()
                .post(post)
                .reporter(reporter)
                .reason(reason)
                .detail(detail)
                .build());
    }

    public long getPendingCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    public long getTotalCount() {
        return reportRepository.count();
    }

    @Override
    public String getReportType() { return "post"; }

    public Page<PostReport> getReportsForAdmin(int page, int size, String statusFilter) {
        PageRequest pageable = PageRequest.of(page, size);
        if ("PENDING".equals(statusFilter)) {
            return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable);
        }
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public Page<PostReport> searchReportsForAdmin(int page, int size, String statusFilter, String keyword) {
        if (keyword == null || keyword.isBlank()) return getReportsForAdmin(page, size, statusFilter);
        PageRequest pageable = PageRequest.of(page, size);
        ReportStatus status = "PENDING".equals(statusFilter) ? ReportStatus.PENDING : null;
        return reportRepository.searchByKeyword(keyword, status, pageable);
    }

    @Override
    @Transactional
    public void deleteContentAndResolve(Long reportId) {
        deletePostAndResolve(reportId);
    }

    @Transactional
    public void deletePostAndResolve(Long reportId) {
        PostReport report = EntityFinder.getOrThrow(reportRepository::findById, reportId, "신고");
        Long postId = report.getPostId();
        postAdminService.deletePost(postId);
        reportRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Integer.MAX_VALUE))
                .stream()
                .filter(r -> r.getPostId().equals(postId))
                .forEach(r -> r.resolve(ReportStatus.POST_DELETED));
    }

    @Transactional
    public void dismissReport(Long reportId) {
        PostReport report = EntityFinder.getOrThrow(reportRepository::findById, reportId, "신고");
        report.resolve(ReportStatus.DISMISSED);
    }

    @Override
    @Transactional
    public void bulkDismiss(List<Long> ids) {
        if (ids.isEmpty()) return;
        reportRepository.findAllById(ids).stream()
                .filter(PostReport::isPending)
                .forEach(r -> r.resolve(ReportStatus.DISMISSED));
    }

    public Map<Long, Long> getAuthorReportCounts(Collection<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return reportRepository.countByPostAuthorIds(userIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    public List<PostReport> getAllPostReportsForExport() {
        return reportRepository.findAllForExport();
    }

    public long getReportCountForUser(Long userId) {
        List<Object[]> result = reportRepository.countByPostAuthorIds(List.of(userId));
        return result.isEmpty() ? 0L : (Long) result.get(0)[1];
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, Long> buildAuthorReportCounts(Page<?> reports) {
        Set<Long> ids = ((Page<PostReport>) reports).getContent().stream()
                .map(PostReport::getPostAuthorId).collect(Collectors.toSet());
        return getAuthorReportCounts(ids);
    }
}
