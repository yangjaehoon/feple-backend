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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

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
            throw new IllegalStateException("이미 신고한 댓글입니다.");
        }
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다: " + commentId));
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + reporterId));

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

    /** 관리자: 댓글 삭제 후 신고 처리 (FK 제약으로 댓글 먼저 모든 신고 삭제) */
    @Transactional
    public void deleteCommentAndResolve(Long reportId) {
        CommentReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("신고를 찾을 수 없습니다: " + reportId));
        Long commentId = report.getComment().getId();
        reportRepository.deleteByCommentId(commentId);
        commentRepository.deleteById(commentId);
    }

    @Transactional
    public void dismissReport(Long reportId) {
        CommentReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("신고를 찾을 수 없습니다: " + reportId));
        report.resolve(ReportStatus.DISMISSED);
    }
}
