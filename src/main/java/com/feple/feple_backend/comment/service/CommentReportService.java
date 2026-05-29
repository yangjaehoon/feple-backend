package com.feple.feple_backend.comment.service;

import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.comment.entity.CommentReport;
import com.feple.feple_backend.comment.repository.CommentReportRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.post.entity.ReportStatus;
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
public class CommentReportService implements ReportAdminService {

    private final CommentReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @Transactional
    public void submitReport(Long commentId, Long reporterId, ReportReason reason, String detail) {
        if (reportRepository.existsByReporterIdAndCommentId(reporterId, commentId)) {
            throw new ConflictException("이미 신고한 댓글입니다.");
        }
        Comment comment = EntityFinder.getOrThrow(commentRepository::findById, commentId, "댓글");
        User reporter = EntityFinder.getOrThrow(userRepository::findById, reporterId, "사용자");

        reportRepository.save(CommentReport.builder()
                .comment(comment)
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

    public Page<CommentReport> getReportsForAdmin(int page, int size, String statusFilter) {
        PageRequest pageable = PageRequest.of(page, size);
        if ("PENDING".equals(statusFilter)) {
            return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable);
        }
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public Page<CommentReport> searchReportsForAdmin(int page, int size, String statusFilter, String keyword) {
        if (keyword == null || keyword.isBlank()) return getReportsForAdmin(page, size, statusFilter);
        PageRequest pageable = PageRequest.of(page, size);
        ReportStatus status = "PENDING".equals(statusFilter) ? ReportStatus.PENDING : null;
        return reportRepository.searchByKeyword(keyword, status, pageable);
    }

    @Transactional
    public void deleteCommentAndResolve(Long reportId) {
        CommentReport report = EntityFinder.getOrThrow(reportRepository::findById, reportId, "신고");
        Long commentId = report.getComment().getId();
        reportRepository.deleteByCommentId(commentId);
        commentRepository.deleteById(commentId);
    }

    @Transactional
    public void dismissReport(Long reportId) {
        CommentReport report = EntityFinder.getOrThrow(reportRepository::findById, reportId, "신고");
        report.resolve(ReportStatus.DISMISSED);
    }

    @Override
    @Transactional
    public void bulkDismiss(List<Long> ids) {
        if (ids.isEmpty()) return;
        reportRepository.findAllById(ids).stream()
                .filter(CommentReport::isPending)
                .forEach(r -> r.resolve(ReportStatus.DISMISSED));
    }

    public Map<Long, Long> getAuthorReportCounts(Collection<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return reportRepository.countByCommentAuthorIds(userIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    public List<CommentReport> getAllCommentReportsForExport() {
        return reportRepository.findAllForExport();
    }

    public long getReportCountForUser(Long userId) {
        List<Object[]> result = reportRepository.countByCommentAuthorIds(List.of(userId));
        return result.isEmpty() ? 0L : (Long) result.get(0)[1];
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, Long> buildAuthorReportCounts(Page<?> reports) {
        Set<Long> ids = ((Page<CommentReport>) reports).getContent().stream()
                .map(CommentReport::getCommentAuthorId).collect(Collectors.toSet());
        return getAuthorReportCounts(ids);
    }
}
