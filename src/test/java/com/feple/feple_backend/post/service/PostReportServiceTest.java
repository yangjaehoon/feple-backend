package com.feple.feple_backend.post.service;

import static com.feple.feple_backend.support.TestEntityFactory.freePost;
import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.post.dto.ReportSubmitRequest;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class PostReportServiceTest {

    @Mock PostReportRepository reportRepository;
    @Mock PostRepository postRepository;
    @Mock PostAdminService postAdminService;
    @Mock UserRepository userRepository;

    @InjectMocks PostReportService postReportService;

    private PostReport pendingReport(Long id, Post post, User reporter) {
        return PostReport.builder()
                .id(id).post(post).reporter(reporter)
                .reason(ReportReason.SPAM).build();
    }

    // ── submitReport ─────────────────────────────────────────────────

    @Test
    void 중복_신고시_ConflictException() {
        given(reportRepository.existsByReporterIdAndPostId(1L, 10L)).willReturn(true);

        assertThatThrownBy(() -> postReportService.submitReport(
                10L, 1L, new ReportSubmitRequest(ReportReason.SPAM, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void 존재하지_않는_게시글_신고시_예외() {
        given(reportRepository.existsByReporterIdAndPostId(1L, 99L)).willReturn(false);
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postReportService.submitReport(
                99L, 1L, new ReportSubmitRequest(ReportReason.SPAM, null)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 정상_신고_저장됨() {
        User author = user(2L);
        Post post = freePost(10L, author);
        User reporter = user(1L);
        given(reportRepository.existsByReporterIdAndPostId(1L, 10L)).willReturn(false);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(reporter));

        postReportService.submitReport(10L, 1L,
                new ReportSubmitRequest(ReportReason.ABUSE, "상세 사유"));

        verify(reportRepository).save(any(PostReport.class));
    }

    // ── deleteContentAndResolve ──────────────────────────────────────

    @Test
    void 게시글_삭제처리시_postAdminService_deletePost_호출됨() {
        User author = user(2L);
        Post post = freePost(10L, author);
        PostReport report = pendingReport(1L, post, user(1L));
        given(reportRepository.findById(1L)).willReturn(Optional.of(report));
        given(reportRepository.findByPostId(10L)).willReturn(List.of(report));

        postReportService.deleteContentAndResolve(1L);

        verify(postAdminService).deletePost(10L);
    }

    @Test
    void 게시글_삭제처리시_동일_게시글_신고들_POST_DELETED로_변경됨() {
        User author = user(2L);
        Post post = freePost(10L, author);
        PostReport report1 = pendingReport(1L, post, user(1L));
        PostReport report2 = pendingReport(2L, post, user(3L));
        given(reportRepository.findById(1L)).willReturn(Optional.of(report1));
        given(reportRepository.findByPostId(10L)).willReturn(List.of(report1, report2));

        postReportService.deleteContentAndResolve(1L);

        assertThat(report1.getStatus()).isEqualTo(ReportStatus.POST_DELETED);
        assertThat(report2.getStatus()).isEqualTo(ReportStatus.POST_DELETED);
    }

    // ── dismissReport ────────────────────────────────────────────────

    @Test
    void 신고_기각시_상태가_REJECTED로_변경됨() {
        User author = user(2L);
        PostReport report = pendingReport(1L, freePost(10L, author), user(1L));
        given(reportRepository.findById(1L)).willReturn(Optional.of(report));

        postReportService.dismissReport(1L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);
    }

    @Test
    void 존재하지_않는_신고_기각시_예외() {
        given(reportRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postReportService.dismissReport(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 이미_처리된_신고_재기각시_예외() {
        User author = user(2L);
        PostReport report = PostReport.builder()
                .id(1L).post(freePost(10L, author)).reporter(user(1L))
                .reason(ReportReason.SPAM).status(ReportStatus.REJECTED).build();
        given(reportRepository.findById(1L)).willReturn(Optional.of(report));

        assertThatThrownBy(() -> postReportService.dismissReport(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 처리된 신고입니다.");
    }

    // ── bulkDismiss ──────────────────────────────────────────────────

    @Test
    void 빈_리스트_bulkDismiss는_no_op() {
        postReportService.bulkDismiss(List.of());

        verify(reportRepository, never()).findAllById(any());
    }

    @Test
    void bulkDismiss_PENDING_신고만_REJECTED로_변경됨() {
        User author = user(2L);
        Post post = freePost(10L, author);
        PostReport pending = pendingReport(1L, post, user(1L));
        // POST_DELETED: PENDING이 아닌 신고 → bulkDismiss가 resolve()를 호출하면 REJECTED로 바뀌어 검출 가능
        PostReport alreadyDeleted = PostReport.builder()
                .id(2L).post(post).reporter(user(3L))
                .reason(ReportReason.SPAM).status(ReportStatus.POST_DELETED).build();

        given(reportRepository.findAllById(List.of(1L, 2L)))
                .willReturn(List.of(pending, alreadyDeleted));

        postReportService.bulkDismiss(List.of(1L, 2L));

        assertThat(pending.getStatus()).isEqualTo(ReportStatus.REJECTED);
        assertThat(alreadyDeleted.getStatus()).isEqualTo(ReportStatus.POST_DELETED);
    }

    // ── getPendingCount ──────────────────────────────────────────────

    @Test
    void getPendingCount_레포지토리에_위임됨() {
        given(reportRepository.countByStatus(ReportStatus.PENDING)).willReturn(3L);

        assertThat(postReportService.getPendingCount()).isEqualTo(3L);
    }

    // ── getTotalCount / getReportType ────────────────────────────────

    @Test
    void getTotalCount_레포지토리에_위임됨() {
        given(reportRepository.count()).willReturn(10L);

        assertThat(postReportService.getTotalCount()).isEqualTo(10L);
    }

    @Test
    void getReportType은_post() {
        assertThat(postReportService.getReportType()).isEqualTo("post");
    }

    // ── findPendingReports / findAllReports / searchReportsByKeyword ──

    @Test
    void 대기중_신고_목록_조회() {
        User author = user(2L);
        PostReport report = pendingReport(1L, freePost(10L, author), user(1L));
        PageRequest pageable = PageRequest.of(0, 20);
        given(reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable))
                .willReturn(new PageImpl<>(List.of(report)));

        Page<PostReport> result = postReportService.findPendingReports(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void 전체_신고_목록_조회() {
        User author = user(2L);
        PostReport report = pendingReport(1L, freePost(10L, author), user(1L));
        PageRequest pageable = PageRequest.of(0, 20);
        given(reportRepository.findAllByOrderByCreatedAtDesc(pageable))
                .willReturn(new PageImpl<>(List.of(report)));

        Page<PostReport> result = postReportService.findAllReports(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void 키워드로_신고_검색() {
        User author = user(2L);
        PostReport report = pendingReport(1L, freePost(10L, author), user(1L));
        PageRequest pageable = PageRequest.of(0, 20);
        given(reportRepository.searchByKeyword("스팸", ReportStatus.PENDING, pageable))
                .willReturn(new PageImpl<>(List.of(report)));

        Page<PostReport> result = postReportService.searchReportsByKeyword("스팸", ReportStatus.PENDING, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    // ── extractAuthorId ───────────────────────────────────────────────

    @Test
    void extractAuthorId는_게시글_작성자ID_반환() {
        User author = user(2L);
        PostReport report = pendingReport(1L, freePost(10L, author), user(1L));

        assertThat(postReportService.extractAuthorId(report)).isEqualTo(2L);
    }

    // ── getAuthorReportCounts ─────────────────────────────────────────

    @Test
    void getAuthorReportCounts_유저ID_비어있으면_빈맵() {
        Map<Long, Long> result = postReportService.getAuthorReportCounts(List.of());

        assertThat(result).isEmpty();
        verify(reportRepository, never()).countByPostAuthorIds(any());
    }

    @Test
    void getAuthorReportCounts_작성자별_신고건수_집계() {
        given(reportRepository.countByPostAuthorIds(List.of(2L)))
                .willReturn(List.<Object[]>of(new Object[]{2L, 3L}));

        Map<Long, Long> result = postReportService.getAuthorReportCounts(List.of(2L));

        assertThat(result).containsEntry(2L, 3L);
    }

    // ── getAllPostReportsForExport ────────────────────────────────────

    @Test
    void 전체_신고_내보내기용_목록_조회() {
        User author = user(2L);
        PostReport report = pendingReport(1L, freePost(10L, author), user(1L));
        given(reportRepository.findAllForExport(any())).willReturn(List.of(report));

        List<PostReport> result = postReportService.getAllPostReportsForExport();

        assertThat(result).hasSize(1);
    }

    // ── getReportCountForUser ─────────────────────────────────────────

    @Test
    void 특정_사용자의_신고건수_조회() {
        given(reportRepository.countByPostAuthorIds(List.of(2L)))
                .willReturn(List.<Object[]>of(new Object[]{2L, 5L}));

        long count = postReportService.getReportCountForUser(2L);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void 신고내역_없는_사용자의_신고건수는_0() {
        given(reportRepository.countByPostAuthorIds(List.of(2L))).willReturn(List.of());

        long count = postReportService.getReportCountForUser(2L);

        assertThat(count).isZero();
    }
}
