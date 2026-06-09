package com.feple.feple_backend.comment.service;

import com.feple.feple_backend.admin.service.ReportSearchParams;
import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.comment.entity.CommentReport;
import com.feple.feple_backend.comment.repository.CommentReportRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.post.dto.SubmitReportCommand;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.admin.service.ReportAdminService;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
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
public class CommentReportService implements ReportAdminService<CommentReport> {

    private final CommentReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public void submitReport(Long commentId, Long reporterId, SubmitReportCommand command) {
        if (reportRepository.existsByReporterIdAndCommentId(reporterId, commentId)) {
            throw new ConflictException("이미 신고한 댓글입니다.");
        }
        Comment comment = EntityFinder.getOrThrow(commentRepository::findById, commentId, "댓글");
        User reporter = EntityFinder.getOrThrow(userRepository::findById, reporterId, "사용자");

        reportRepository.save(CommentReport.builder()
                .comment(comment)
                .reporter(reporter)
                .reason(command.reason())
                .detail(command.detail())
                .build());
    }

    @Cacheable(value = "adminReportTypeCounts", key = "'commentPending'")
    public long getPendingCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    @Cacheable(value = "adminReportTypeCounts", key = "'commentTotal'")
    public long getTotalCount() {
        return reportRepository.count();
    }

    @Override
    public String getReportType() { return "comment"; }

    public Page<CommentReport> getReportsForAdmin(int page, int size, String statusFilter) {
        PageRequest pageable = PageRequest.of(page, size);
        if ("PENDING".equals(statusFilter)) {
            return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable);
        }
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public Page<CommentReport> searchReportsForAdmin(ReportSearchParams params) {
        if (params.keyword() == null || params.keyword().isBlank()) return getReportsForAdmin(params.page(), params.size(), params.statusFilter());
        PageRequest pageable = PageRequest.of(params.page(), params.size());
        ReportStatus status = "PENDING".equals(params.statusFilter()) ? ReportStatus.PENDING : null;
        return reportRepository.searchByKeyword(params.keyword(), status, pageable);
    }

    @Override
    @Transactional
    public void deleteContentAndResolve(Long reportId) {
        deleteCommentAndResolve(reportId);
    }

    @Transactional
    public void deleteCommentAndResolve(Long reportId) {
        CommentReport report = EntityFinder.getOrThrow(reportRepository::findById, reportId, "신고");
        Long commentId = report.getCommentId();
        Comment comment = EntityFinder.getOrThrow(commentRepository::findById, commentId, "댓글");
        Post post = EntityFinder.getOrThrow(postRepository::findById, comment.getPostId(), "게시글");
        reportRepository.deleteByCommentId(commentId);
        commentRepository.deleteById(commentId);
        post.decrementCommentCount();
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
    public Map<Long, Long> buildAuthorReportCounts(Page<CommentReport> reports) {
        Set<Long> ids = reports.getContent().stream()
                .map(CommentReport::getCommentAuthorId).collect(Collectors.toSet());
        return getAuthorReportCounts(ids);
    }
}
