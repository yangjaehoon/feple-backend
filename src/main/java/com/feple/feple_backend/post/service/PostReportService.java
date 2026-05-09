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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public Page<PostReport> getReportsForAdmin(int page, int size, String statusFilter) {
        PageRequest pageable = PageRequest.of(page, size);
        if ("PENDING".equals(statusFilter)) {
            return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable);
        }
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable);
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
}
