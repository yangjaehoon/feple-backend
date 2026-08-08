package com.feple.feple_backend.comment.service;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.service.ReportAdminService;
import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.comment.entity.CommentReport;
import com.feple.feple_backend.comment.repository.CommentReportRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.QueryResultMapper;
import com.feple.feple_backend.global.ReportRejectionService;
import com.feple.feple_backend.global.cache.EvictAdminReportCaches;
import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.post.dto.ReportSubmitRequest;
import com.feple.feple_backend.post.service.PostService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentReportService implements ReportAdminService<CommentReport> {

    private final CommentReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final CommentDeleter commentDeleter;
    private final PostService postService;
    private final UserRepository userRepository;

    // PostReportService/ArtistPhotoReportService의 submitReport와 구조가 동일하지만,
    // 빌더 타입이 전부 달라 제네릭으로 묶으면 콜백만 많아지고 오히려 읽기 어려워져 통합하지 않는다.
    @Transactional
    @EvictAdminReportCaches
    public void submitReport(Long commentId, Long reporterId, ReportSubmitRequest command) {
        if (reportRepository.existsByReporterIdAndCommentId(reporterId, commentId)) {
            throw new ConflictException("이미 신고한 댓글입니다.");
        }
        // 이미 블라인드된 댓글도 추가 신고를 받을 수 있어야 하므로 조회는 제약을 우회한다.
        Comment comment = EntityLoader.getOrThrow(commentRepository::findByIdIgnoringRestrictions, commentId, "댓글");
        User reporter = EntityLoader.getOrThrow(userRepository::findById, reporterId, "사용자");

        reportRepository.save(CommentReport.builder()
                .comment(comment)
                .reporter(reporter)
                .reason(command.reason())
                .detail(command.detail())
                .build());

        autoBlindIfThresholdReached(comment);
    }

    // 신고(대기 상태)가 임계치 이상 쌓이면 관리자 검토 전이라도 자동으로 블라인드 처리한다.
    private void autoBlindIfThresholdReached(Comment comment) {
        if (comment.isBlinded()) return;
        long pendingCount = reportRepository.countByCommentIdAndStatus(comment.getId(), ReportStatus.PENDING);
        if (pendingCount >= AdminConstants.AUTO_BLIND_REPORT_THRESHOLD) {
            comment.blind();
        }
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

    // 댓글은 CommentDeleter.deleteSingle을 통해 소프트 삭제되며(Comment의 @SQLDelete),
    // 신고 레코드는 함께 하드 삭제됨(PostReportService처럼 레코드 보존+resolve가 아님) —
    // ReportCommandService.deleteContentAndResolve 계약 문서 참고
    @Override
    @EvictAdminReportCaches
    @Transactional
    public void deleteContentAndResolve(Long reportId) {
        CommentReport report = EntityLoader.getOrThrow(reportRepository::findById, reportId, "신고");
        Long commentId = report.getCommentId();
        // 블라인드된 댓글도 관리자는 삭제할 수 있어야 하므로 조회는 제약을 우회한다.
        Comment comment = EntityLoader.getOrThrow(commentRepository::findByIdIgnoringRestrictions, commentId, "댓글");
        commentDeleter.deleteSingle(commentId);
        postService.decrementCommentCount(comment.getPostId());
    }

    @EvictAdminReportCaches
    @Transactional
    public void dismissReport(Long reportId) {
        CommentReport report = ReportRejectionService.reject(reportRepository, reportId);
        unblindIfBelowThreshold(report.getCommentId());
    }

    @Override
    @EvictAdminReportCaches
    @Transactional
    public void bulkDismiss(List<Long> ids) {
        if (ids.isEmpty()) return;
        List<CommentReport> rejected = ReportRejectionService.bulkDismiss(reportRepository, ids);
        rejected.stream().map(CommentReport::getCommentId).distinct().forEach(this::unblindIfBelowThreshold);
    }

    // 신고를 반려해 남은 대기 신고가 임계치 아래로 내려가면 블라인드를 해제한다.
    private void unblindIfBelowThreshold(Long commentId) {
        long pendingCount = reportRepository.countByCommentIdAndStatus(commentId, ReportStatus.PENDING);
        if (pendingCount < AdminConstants.AUTO_BLIND_REPORT_THRESHOLD) {
            commentRepository.findByIdIgnoringRestrictions(commentId).ifPresent(Comment::unblind);
        }
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
