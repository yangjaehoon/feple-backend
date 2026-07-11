package com.feple.feple_backend.comment.service;

import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.comment.entity.CommentReport;
import com.feple.feple_backend.comment.repository.CommentLikeRepository;
import com.feple.feple_backend.comment.repository.CommentReportRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.post.dto.SubmitReportCommand;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.service.ReportAdminService;
import com.feple.feple_backend.global.QueryResultMapper;
import com.feple.feple_backend.global.EntityRequirer;
import com.feple.feple_backend.global.ReportRejectionService;
import com.feple.feple_backend.global.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import com.feple.feple_backend.global.cache.EvictAdminReportCaches;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentReportService implements ReportAdminService<CommentReport> {

    private final CommentReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public void submitReport(Long commentId, Long reporterId, SubmitReportCommand command) {
        if (reportRepository.existsByReporterIdAndCommentId(reporterId, commentId)) {
            throw new ConflictException("이미 신고한 댓글입니다.");
        }
        Comment comment = EntityRequirer.getOrThrow(commentRepository::findById, commentId, "댓글");
        User reporter = EntityRequirer.getOrThrow(userRepository::findById, reporterId, "사용자");

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

    @Override
    public Page<CommentReport> findPendingReports(PageRequest pageable) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable);
    }

    @Override
    public Page<CommentReport> findAllReports(PageRequest pageable) {
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public Page<CommentReport> searchReportsByKeyword(String keyword, ReportStatus status, PageRequest pageable) {
        return reportRepository.searchByKeyword(keyword, status, pageable);
    }

    @Override
    @EvictAdminReportCaches
    @Transactional
    public void deleteContentAndResolve(Long reportId) {
        CommentReport report = EntityRequirer.getOrThrow(reportRepository::findById, reportId, "신고");
        Long commentId = report.getCommentId();
        Comment comment = EntityRequirer.getOrThrow(commentRepository::findById, commentId, "댓글");
        commentLikeRepository.deleteByCommentId(commentId);
        reportRepository.deleteByCommentId(commentId);
        commentRepository.deleteById(commentId);
        postRepository.decrementCommentCount(comment.getPostId());
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
    public Long extractAuthorId(CommentReport report) { return report.getCommentAuthorId(); }

    @Override
    public Map<Long, Long> getAuthorReportCounts(Collection<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return QueryResultMapper.toLongMap(reportRepository.countByCommentAuthorIds(userIds));
    }

    public List<CommentReport> getAllCommentReportsForExport() {
        return reportRepository.findAllForExport(PageRequest.of(0, AdminConstants.MAX_EXPORT_ROWS));
    }

    public long getReportCountForUser(Long userId) {
        return QueryResultMapper.extractSingleCount(reportRepository.countByCommentAuthorIds(List.of(userId)));
    }
}
