package com.feple.feple_backend.post.service;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.service.ReportAdminService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.QueryResultMapper;
import com.feple.feple_backend.global.ReportRejectionService;
import com.feple.feple_backend.global.cache.EvictAdminReportCaches;
import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.post.dto.ReportSubmitRequest;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReportService implements ReportAdminService<PostReport> {

    private final PostReportRepository reportRepository;
    private final PostRepository postRepository;
    private final PostAdminService postAdminService;
    private final UserRepository userRepository;

    // CommentReportService/ArtistPhotoReportService의 submitReport와 구조가 동일하지만,
    // 빌더 타입이 전부 달라 제네릭으로 묶으면 콜백만 많아지고 오히려 읽기 어려워져 통합하지 않는다.
    @Transactional
    @EvictAdminReportCaches
    public void submitReport(Long postId, Long reporterId, ReportSubmitRequest command) {
        if (reportRepository.existsByReporterIdAndPostId(reporterId, postId)) {
            throw new ConflictException("이미 신고한 게시글입니다.");
        }
        Post post = EntityLoader.getOrThrow(postRepository::findById, postId, "게시글");
        User reporter = EntityLoader.getOrThrow(userRepository::findById, reporterId, "사용자");

        // existsBy 체크 후 save() 사이의 TOCTOU 레이스(동시 중복 신고)는 유니크 제약(reporter_id, post_id)이
        // 최종 방어선이다 — 위 existsBy와 동일한 메시지의 ConflictException으로 변환해준다.
        try {
            reportRepository.save(PostReport.builder()
                    .post(post)
                    .reporter(reporter)
                    .reason(command.reason())
                    .detail(command.detail())
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("이미 신고한 게시글입니다.");
        }

        autoBlindIfThresholdReached(post);
    }

    // 신고(대기 상태)가 임계치 이상 쌓이면 관리자 검토 전이라도 자동으로 블라인드 처리한다.
    private void autoBlindIfThresholdReached(Post post) {
        if (post.isBlinded()) return;
        long pendingCount = reportRepository.countByPostIdAndStatus(post.getId(), ReportStatus.PENDING);
        if (pendingCount >= AdminConstants.AUTO_BLIND_REPORT_THRESHOLD) {
            post.blind();
        }
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

    // 게시글은 소프트 삭제(행 보존)라 신고 레코드도 함께 보존하고 상태만 갱신 —
    // 하드 삭제하는 CommentReportService/ArtistPhotoReportService와 의도적으로 다름
    // (ReportCommandService.deleteContentAndResolve 계약 문서 참고)
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
        PostReport report = ReportRejectionService.reject(reportRepository, reportId);
        unblindIfBelowThreshold(report.getPostId());
    }

    @Override
    @EvictAdminReportCaches
    @Transactional
    public void bulkDismiss(List<Long> ids) {
        if (ids.isEmpty()) return;
        List<PostReport> rejected = ReportRejectionService.bulkDismiss(reportRepository, ids);
        List<Long> postIds = rejected.stream().map(PostReport::getPostId).distinct().toList();
        unblindAllBelowThreshold(postIds);
    }

    // 신고를 반려해 남은 대기 신고가 임계치 아래로 내려가면 블라인드를 해제한다.
    private void unblindIfBelowThreshold(Long postId) {
        long pendingCount = reportRepository.countByPostIdAndStatus(postId, ReportStatus.PENDING);
        if (pendingCount < AdminConstants.AUTO_BLIND_REPORT_THRESHOLD) {
            postRepository.findById(postId).ifPresent(Post::unblind);
        }
    }

    // unblindIfBelowThreshold의 배치 버전 — bulkDismiss는 최대 20건(관리자 페이지 크기)이 한 번에
    // 반려되는데, 게시글마다 대기 신고 수 조회+단건 블라인드 해제 조회를 반복하면 최대 40쿼리가 발생하므로
    // 그룹 집계 1쿼리 + IN 조회 1쿼리로 묶는다.
    private void unblindAllBelowThreshold(List<Long> postIds) {
        if (postIds.isEmpty()) return;
        Map<Long, Long> pendingCountsByPostId =
                QueryResultMapper.toLongMap(reportRepository.countByPostIdInAndStatus(postIds, ReportStatus.PENDING));
        List<Long> toUnblind = postIds.stream()
                .filter(postId -> pendingCountsByPostId.getOrDefault(postId, 0L) < AdminConstants.AUTO_BLIND_REPORT_THRESHOLD)
                .toList();
        if (toUnblind.isEmpty()) return;
        postRepository.findAllById(toUnblind).forEach(Post::unblind);
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
