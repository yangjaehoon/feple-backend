package com.feple.feple_backend.comment.service;

import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.comment.entity.CommentReport;
import com.feple.feple_backend.comment.repository.CommentReportRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.post.dto.ReportSubmitRequest;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.post.service.PostService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentReportServiceTest {

    @Mock CommentReportRepository reportRepository;
    @Mock CommentRepository commentRepository;
    @Mock CommentDeleter commentDeleter;
    @Mock PostService postService;
    @Mock UserRepository userRepository;

    @InjectMocks CommentReportService commentReportService;

    private Comment mockComment() {
        return mock(Comment.class);
    }

    private Comment mockCommentWithId(Long id) {
        Comment comment = mock(Comment.class);
        given(comment.getId()).willReturn(id);
        return comment;
    }

    private CommentReport pendingReport(Long id, Comment comment, User reporter) {
        return CommentReport.builder()
                .id(id).comment(comment).reporter(reporter)
                .reason(ReportReason.SPAM).build();
    }

    // ── submitReport ─────────────────────────────────────────────────

    @Test
    void 중복_신고시_ConflictException() {
        given(reportRepository.existsByReporterIdAndCommentId(1L, 10L)).willReturn(true);

        assertThatThrownBy(() -> commentReportService.submitReport(
                10L, 1L, new ReportSubmitRequest(ReportReason.SPAM, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void 존재하지_않는_댓글_신고시_예외() {
        given(reportRepository.existsByReporterIdAndCommentId(1L, 99L)).willReturn(false);
        given(commentRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentReportService.submitReport(
                99L, 1L, new ReportSubmitRequest(ReportReason.SPAM, null)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 존재하지_않는_사용자가_신고시_예외() {
        Comment comment = mockComment();
        given(reportRepository.existsByReporterIdAndCommentId(99L, 10L)).willReturn(false);
        given(commentRepository.findById(10L)).willReturn(Optional.of(comment));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentReportService.submitReport(
                10L, 99L, new ReportSubmitRequest(ReportReason.ABUSE, null)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 정상_신고_저장됨() {
        Comment comment = mockComment();
        User reporter = user(1L);
        given(reportRepository.existsByReporterIdAndCommentId(1L, 10L)).willReturn(false);
        given(commentRepository.findById(10L)).willReturn(Optional.of(comment));
        given(userRepository.findById(1L)).willReturn(Optional.of(reporter));

        commentReportService.submitReport(10L, 1L,
                new ReportSubmitRequest(ReportReason.SPAM, "상세 사유"));

        verify(reportRepository).save(any(CommentReport.class));
    }

    @Test
    void 신고_동시요청으로_유니크제약_위반시_ConflictException으로_변환() {
        Comment comment = mockComment();
        User reporter = user(1L);
        given(reportRepository.existsByReporterIdAndCommentId(1L, 10L)).willReturn(false);
        given(commentRepository.findById(10L)).willReturn(Optional.of(comment));
        given(userRepository.findById(1L)).willReturn(Optional.of(reporter));
        given(reportRepository.save(any(CommentReport.class)))
                .willThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> commentReportService.submitReport(
                10L, 1L, new ReportSubmitRequest(ReportReason.SPAM, "상세 사유")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("이미 신고한");
    }

    // ── deleteContentAndResolve ──────────────────────────────────────

    @Test
    void 댓글_삭제처리시_신고와_댓글_모두_삭제됨() {
        Comment comment = mockCommentWithId(10L);
        given(comment.getPostId()).willReturn(5L);
        User reporter = user(1L);
        CommentReport report = pendingReport(1L, comment, reporter);
        given(reportRepository.findById(1L)).willReturn(Optional.of(report));
        given(commentRepository.findById(10L)).willReturn(Optional.of(comment));

        commentReportService.deleteContentAndResolve(1L);

        verify(commentDeleter).deleteSingle(10L);
        verify(postService).decrementCommentCount(5L);
    }

    // ── dismissReport ────────────────────────────────────────────────

    @Test
    void 신고_기각시_상태가_REJECTED로_변경됨() {
        Comment comment = mockComment();
        CommentReport report = pendingReport(1L, comment, user(1L));
        given(reportRepository.findById(1L)).willReturn(Optional.of(report));

        commentReportService.dismissReport(1L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.REJECTED);
    }

    @Test
    void 존재하지_않는_신고_기각시_예외() {
        given(reportRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentReportService.dismissReport(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── bulkDismiss ──────────────────────────────────────────────────

    @Test
    void 빈_리스트_bulkDismiss는_no_op() {
        commentReportService.bulkDismiss(List.of());

        verify(reportRepository, never()).findAllById(any());
    }

    @Test
    void bulkDismiss_PENDING_신고만_REJECTED로_변경됨() {
        Comment comment = mockComment();
        CommentReport pending = pendingReport(1L, comment, user(1L));
        // POST_DELETED: PENDING이 아닌 신고 → bulkDismiss가 resolve()를 호출하면 REJECTED로 바뀌어 검출 가능
        CommentReport alreadyProcessed = CommentReport.builder()
                .id(2L).comment(comment).reporter(user(2L))
                .reason(ReportReason.SPAM).status(ReportStatus.POST_DELETED).build();

        given(reportRepository.findAllById(List.of(1L, 2L)))
                .willReturn(List.of(pending, alreadyProcessed));

        commentReportService.bulkDismiss(List.of(1L, 2L));

        assertThat(pending.getStatus()).isEqualTo(ReportStatus.REJECTED);
        assertThat(alreadyProcessed.getStatus()).isEqualTo(ReportStatus.POST_DELETED);
    }

    // ── getPendingCount ──────────────────────────────────────────────

    @Test
    void getPendingCount_레포지토리에_위임됨() {
        given(reportRepository.countByStatus(ReportStatus.PENDING)).willReturn(5L);

        assertThat(commentReportService.getPendingCount()).isEqualTo(5L);
    }

    @Test
    void getTotalCount_레포지토리에_위임됨() {
        given(reportRepository.count()).willReturn(9L);

        assertThat(commentReportService.getTotalCount()).isEqualTo(9L);
    }

    @Test
    void getReportType은_comment() {
        assertThat(commentReportService.getReportType()).isEqualTo("comment");
    }

    @Test
    void findPendingReports는_레포지토리에_위임() {
        org.springframework.data.domain.PageRequest pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<CommentReport> page = new org.springframework.data.domain.PageImpl<>(List.of());
        given(reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable)).willReturn(page);

        assertThat(commentReportService.findPendingReports(pageable)).isSameAs(page);
    }

    @Test
    void findAllReports는_레포지토리에_위임() {
        org.springframework.data.domain.PageRequest pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<CommentReport> page = new org.springframework.data.domain.PageImpl<>(List.of());
        given(reportRepository.findAllByOrderByCreatedAtDesc(pageable)).willReturn(page);

        assertThat(commentReportService.findAllReports(pageable)).isSameAs(page);
    }

    @Test
    void searchReportsByKeyword는_레포지토리에_위임() {
        org.springframework.data.domain.PageRequest pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<CommentReport> page = new org.springframework.data.domain.PageImpl<>(List.of());
        given(reportRepository.searchByKeyword("키워드", ReportStatus.PENDING, pageable)).willReturn(page);

        assertThat(commentReportService.searchReportsByKeyword("키워드", ReportStatus.PENDING, pageable)).isSameAs(page);
    }

    @Test
    void extractAuthorId는_댓글작성자_ID_반환() {
        Comment comment = mockComment();
        given(comment.getUserId()).willReturn(7L);
        CommentReport report = pendingReport(1L, comment, user(1L));

        assertThat(commentReportService.extractAuthorId(report)).isEqualTo(7L);
    }

    @Test
    void getAuthorReportCounts_유저ID_비어있으면_빈맵() {
        assertThat(commentReportService.getAuthorReportCounts(List.of())).isEmpty();
        verify(reportRepository, never()).countByCommentAuthorIds(any());
    }

    @Test
    void getAuthorReportCounts_유저별_신고건수_맵_반환() {
        given(reportRepository.countByCommentAuthorIds(List.of(7L)))
                .willReturn(List.<Object[]>of(new Object[]{7L, 2L}));

        assertThat(commentReportService.getAuthorReportCounts(List.of(7L))).containsEntry(7L, 2L);
    }

    @Test
    void getAllCommentReportsForExport_레포지토리에_위임() {
        given(reportRepository.findAllForExport(any())).willReturn(List.of());

        assertThat(commentReportService.getAllCommentReportsForExport()).isEmpty();
    }

    @Test
    void getReportCountForUser_단건_카운트_반환() {
        given(reportRepository.countByCommentAuthorIds(List.of(7L)))
                .willReturn(List.<Object[]>of(new Object[]{7L, 4L}));

        assertThat(commentReportService.getReportCountForUser(7L)).isEqualTo(4L);
    }

    @Test
    void removeReportsByReporter_신고자_기준_삭제를_레포지토리에_위임() {
        commentReportService.removeReportsByReporter(7L);

        verify(reportRepository).deleteByReporterId(7L);
    }
}
