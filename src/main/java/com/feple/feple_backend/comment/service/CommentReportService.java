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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
