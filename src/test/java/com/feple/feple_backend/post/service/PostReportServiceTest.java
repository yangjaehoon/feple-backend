package com.feple.feple_backend.post.service;

import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.post.dto.SubmitReportCommand;
import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostReportServiceTest {

    @Mock PostReportRepository reportRepository;
    @Mock PostRepository postRepository;
    @Mock PostAdminService postAdminService;
    @Mock UserRepository userRepository;

    @InjectMocks PostReportService postReportService;

    private User user(Long id) {
        return User.builder().id(id).nickname("user" + id)
                .oauthId("o" + id).role(UserRole.USER).build();
    }

    private Post post(Long id, User author) {
        return Post.builder()
                .id(id).title("제목").content("내용")
                .user(author).boardType(BoardType.FREE)
                .likeCount(0).scrapCount(0)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

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
                10L, 1L, new SubmitReportCommand(ReportReason.SPAM, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void 존재하지_않는_게시글_신고시_예외() {
        given(reportRepository.existsByReporterIdAndPostId(1L, 99L)).willReturn(false);
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postReportService.submitReport(
                99L, 1L, new SubmitReportCommand(ReportReason.SPAM, null)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 정상_신고_저장됨() {
        User author = user(2L);
        Post post = post(10L, author);
        User reporter = user(1L);
        given(reportRepository.existsByReporterIdAndPostId(1L, 10L)).willReturn(false);
        given(postRepository.findById(10L)).willReturn(Optional.of(post));
        given(userRepository.findById(1L)).willReturn(Optional.of(reporter));

        postReportService.submitReport(10L, 1L,
                new SubmitReportCommand(ReportReason.ABUSE, "상세 사유"));

        verify(reportRepository).save(any(PostReport.class));
    }

    // ── deletePostAndResolve ─────────────────────────────────────────

    @Test
    void 게시글_삭제처리시_postAdminService_deletePost_호출됨() {
        User author = user(2L);
        Post post = post(10L, author);
        PostReport report = pendingReport(1L, post, user(1L));
        given(reportRepository.findById(1L)).willReturn(Optional.of(report));
        given(reportRepository.findByPostId(10L)).willReturn(List.of(report));

        postReportService.deletePostAndResolve(1L);

        verify(postAdminService).deletePost(10L);
    }

    @Test
    void 게시글_삭제처리시_동일_게시글_신고들_POST_DELETED로_변경됨() {
        User author = user(2L);
        Post post = post(10L, author);
        PostReport report1 = pendingReport(1L, post, user(1L));
        PostReport report2 = pendingReport(2L, post, user(3L));
        given(reportRepository.findById(1L)).willReturn(Optional.of(report1));
        given(reportRepository.findByPostId(10L)).willReturn(List.of(report1, report2));

        postReportService.deletePostAndResolve(1L);

        assertThat(report1.getStatus()).isEqualTo(ReportStatus.POST_DELETED);
        assertThat(report2.getStatus()).isEqualTo(ReportStatus.POST_DELETED);
    }

    // ── dismissReport ────────────────────────────────────────────────

    @Test
    void 신고_기각시_상태가_DISMISSED로_변경됨() {
        User author = user(2L);
        PostReport report = pendingReport(1L, post(10L, author), user(1L));
        given(reportRepository.findById(1L)).willReturn(Optional.of(report));

        postReportService.dismissReport(1L);

        assertThat(report.getStatus()).isEqualTo(ReportStatus.DISMISSED);
    }

    @Test
    void 존재하지_않는_신고_기각시_예외() {
        given(reportRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postReportService.dismissReport(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── bulkDismiss ──────────────────────────────────────────────────

    @Test
    void 빈_리스트_bulkDismiss는_no_op() {
        postReportService.bulkDismiss(List.of());

        verify(reportRepository, never()).findAllById(any());
    }

    @Test
    void bulkDismiss_PENDING_신고만_DISMISSED로_변경됨() {
        User author = user(2L);
        Post post = post(10L, author);
        PostReport pending = pendingReport(1L, post, user(1L));
        PostReport alreadyDismissed = PostReport.builder()
                .id(2L).post(post).reporter(user(3L))
                .reason(ReportReason.SPAM).status(ReportStatus.DISMISSED).build();

        given(reportRepository.findAllById(List.of(1L, 2L)))
                .willReturn(List.of(pending, alreadyDismissed));

        postReportService.bulkDismiss(List.of(1L, 2L));

        assertThat(pending.getStatus()).isEqualTo(ReportStatus.DISMISSED);
        assertThat(alreadyDismissed.getStatus()).isEqualTo(ReportStatus.DISMISSED);
    }

    // ── getPendingCount ──────────────────────────────────────────────

    @Test
    void getPendingCount_레포지토리에_위임됨() {
        given(reportRepository.countByStatus(ReportStatus.PENDING)).willReturn(3L);

        assertThat(postReportService.getPendingCount()).isEqualTo(3L);
    }
}
