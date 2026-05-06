package com.feple.feple_backend.post.service;

import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostReportService {

    private final PostReportRepository reportRepository;
    private final PostRepository postRepository;
    private final PostService postService;
    private final UserRepository userRepository;

    /** 신고 접수 */
    @Transactional
    public void submitReport(Long postId, Long reporterId, ReportReason reason, String detail) {
        if (reportRepository.existsByReporterIdAndPostId(reporterId, postId)) {
            throw new IllegalStateException("이미 신고한 게시글입니다.");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("게시글을 찾을 수 없습니다: " + postId));
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + reporterId));

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

    /** 관리자: 신고 목록 조회 */
    public Page<PostReport> getReportsForAdmin(int page, int size, String statusFilter) {
        PageRequest pageable = PageRequest.of(page, size);
        if ("PENDING".equals(statusFilter)) {
            return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable);
        }
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    /** 관리자: 게시글 삭제 후 신고 처리 */
    @Transactional
    public void deletePostAndResolve(Long reportId) {
        PostReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("신고를 찾을 수 없습니다: " + reportId));
        postService.deletePost(report.getPost().getId());
        // 해당 게시글의 다른 신고도 모두 처리
        reportRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, Integer.MAX_VALUE))
                .stream()
                .filter(r -> r.getPost().getId().equals(report.getPost().getId()))
                .forEach(r -> r.resolve(ReportStatus.POST_DELETED));
    }

    /** 관리자: 신고 기각 */
    @Transactional
    public void dismissReport(Long reportId) {
        PostReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NoSuchElementException("신고를 찾을 수 없습니다: " + reportId));
        report.resolve(ReportStatus.DISMISSED);
    }
}
