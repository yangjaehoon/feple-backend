package com.feple.feple_backend.comment.service;

import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.comment.entity.CommentReport;
import com.feple.feple_backend.comment.repository.CommentReportRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.post.dto.SubmitReportCommand;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentReportServiceTest {

    @Mock CommentReportRepository reportRepository;
    @Mock CommentRepository commentRepository;
    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;

    @InjectMocks CommentReportService commentReportService;

    private User user(Long id) {
        return User.builder().id(id).nickname("user" + id)
                .oauthId("o" + id).role(UserRole.USER).build();
    }

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
                10L, 1L, new SubmitReportCommand(ReportReason.SPAM, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void 존재하지_않는_댓글_신고시_예외() {
        given(reportRepository.existsByReporterIdAndCommentId(1L, 99L)).willReturn(false);
        given(commentRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentReportService.submitReport(
                99L, 1L, new SubmitReportCommand(ReportReason.SPAM, null)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 존재하지_않는_사용자가_신고시_예외() {
        Comment comment = mockComment();
        given(reportRepository.existsByReporterIdAndCommentId(99L, 10L)).willReturn(false);
        given(commentRepository.findById(10L)).willReturn(Optional.of(comment));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentReportService.submitReport(
                10L, 99L, new SubmitReportCommand(ReportReason.ABUSE, null)))
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
                new SubmitReportCommand(ReportReason.SPAM, "상세 사유"));

        verify(reportRepository).save(any(CommentReport.class));
    }

    // ── deleteCommentAndResolve ──────────────────────────────────────

    @Test
    void 댓글_삭제처리시_신고와_댓글_모두_삭제됨() {
        Comment comment = mockCommentWithId(10L);
        given(comment.getPostId()).willReturn(5L);
        Post post = mock(Post.class);
        User reporter = user(1L);
        CommentReport report = pendingReport(1L, comment, reporter);
        given(reportRepository.findById(1L)).willReturn(Optional.of(report));
        given(commentRepository.findById(10L)).willReturn(Optional.of(comment));
        given(postRepository.findById(5L)).willReturn(Optional.of(post));

        commentReportService.deleteCommentAndResolve(1L);

        verify(reportRepository).deleteByCommentId(10L);
        verify(commentRepository).deleteById(10L);
        verify(post).decrementCommentCount();
    }

    // ── dismissReport ────────────────────────────────────────────────

    @Test
    void 신고_기각시_상태가_DISMISSED로_변경됨() {
        Comment comment = mockComment();
        CommentReport report = pendingReport(1L, comment, user(1L));
        given(reportRepository.findById(1L)).willReturn(Optional.of(report));

        commentReportService.dismissReport(1L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.DISMISSED);
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
    void bulkDismiss_PENDING_신고만_DISMISSED로_변경됨() {
        Comment comment = mockComment();
        CommentReport pending = pendingReport(1L, comment, user(1L));
        CommentReport alreadyDismissed = CommentReport.builder()
                .id(2L).comment(comment).reporter(user(2L))
                .reason(ReportReason.SPAM).status(ReportStatus.DISMISSED).build();

        given(reportRepository.findAllById(List.of(1L, 2L)))
                .willReturn(List.of(pending, alreadyDismissed));

        commentReportService.bulkDismiss(List.of(1L, 2L));

        assertThat(pending.getStatus()).isEqualTo(ReportStatus.DISMISSED);
        assertThat(alreadyDismissed.getStatus()).isEqualTo(ReportStatus.DISMISSED);
    }

    // ── getPendingCount ──────────────────────────────────────────────

    @Test
    void getPendingCount_레포지토리에_위임됨() {
        given(reportRepository.countByStatus(ReportStatus.PENDING)).willReturn(5L);

        assertThat(commentReportService.getPendingCount()).isEqualTo(5L);
    }
}
