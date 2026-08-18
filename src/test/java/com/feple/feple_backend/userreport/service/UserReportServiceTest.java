package com.feple.feple_backend.userreport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.post.dto.ReportSubmitRequest;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.user.service.UserAdminService;
import com.feple.feple_backend.userreport.entity.UserReport;
import com.feple.feple_backend.userreport.repository.UserReportRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserReportServiceTest {

    @Mock UserReportRepository reportRepository;
    @Mock UserRepository userRepository;
    @Mock UserAdminService userAdminService;

    @InjectMocks UserReportService service;

    private User user(Long id) {
        return User.builder().id(id).nickname("user" + id).build();
    }

    private UserReport report(Long id, User target, User reporter) {
        UserReport report = UserReport.builder()
                .target(target)
                .reporter(reporter)
                .reason(ReportReason.ABUSE)
                .detail("상세")
                .build();
        ReflectionTestUtils.setField(report, "id", id);
        return report;
    }

    // ── submitReport ─────────────────────────────────────────────────────

    @Test
    void 신고_정상_제출() {
        User target = user(10L);
        User reporter = user(20L);
        given(reportRepository.existsByReporterIdAndTargetId(20L, 10L)).willReturn(false);
        given(userRepository.findById(10L)).willReturn(Optional.of(target));
        given(userRepository.findById(20L)).willReturn(Optional.of(reporter));

        service.submitReport(10L, 20L, new ReportSubmitRequest(ReportReason.ABUSE, "상세"));

        verify(reportRepository).save(any(UserReport.class));
    }

    @Test
    void 신고_자기자신이면_예외() {
        assertThatThrownBy(() -> service.submitReport(20L, 20L, new ReportSubmitRequest(ReportReason.ABUSE, "상세")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("자기 자신");
        verify(reportRepository, never()).save(any());
    }

    @Test
    void 신고_이미_신고한_사용자면_예외() {
        given(reportRepository.existsByReporterIdAndTargetId(20L, 10L)).willReturn(true);

        assertThatThrownBy(() -> service.submitReport(10L, 20L, new ReportSubmitRequest(ReportReason.ABUSE, "상세")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("이미 신고한");
        verify(reportRepository, never()).save(any());
    }

    @Test
    void 신고_동시요청으로_유니크제약_위반시_ConflictException으로_변환() {
        User target = user(10L);
        User reporter = user(20L);
        given(reportRepository.existsByReporterIdAndTargetId(20L, 10L)).willReturn(false);
        given(userRepository.findById(10L)).willReturn(Optional.of(target));
        given(userRepository.findById(20L)).willReturn(Optional.of(reporter));
        given(reportRepository.save(any(UserReport.class)))
                .willThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> service.submitReport(10L, 20L, new ReportSubmitRequest(ReportReason.ABUSE, "상세")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("이미 신고한");
    }

    // ── 카운트/조회 위임 ─────────────────────────────────────────────────

    @Test
    void getPendingCount는_PENDING_상태_카운트_위임() {
        given(reportRepository.countByStatus(ReportStatus.PENDING)).willReturn(3L);

        assertThat(service.getPendingCount()).isEqualTo(3L);
    }

    @Test
    void getTotalCount는_전체_카운트_위임() {
        given(reportRepository.count()).willReturn(7L);

        assertThat(service.getTotalCount()).isEqualTo(7L);
    }

    @Test
    void getReportType은_user_반환() {
        assertThat(service.getReportType()).isEqualTo("user");
    }

    @Test
    void findPendingReports는_리포지토리에_위임() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<UserReport> page = new PageImpl<>(List.of());
        given(reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable)).willReturn(page);

        assertThat(service.findPendingReports(pageable)).isSameAs(page);
    }

    @Test
    void findAllReports는_리포지토리에_위임() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<UserReport> page = new PageImpl<>(List.of());
        given(reportRepository.findAllByOrderByCreatedAtDesc(pageable)).willReturn(page);

        assertThat(service.findAllReports(pageable)).isSameAs(page);
    }

    @Test
    void searchReportsByKeyword는_리포지토리에_위임() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<UserReport> page = new PageImpl<>(List.of());
        given(reportRepository.searchByKeyword("키워드", ReportStatus.PENDING, pageable)).willReturn(page);

        assertThat(service.searchReportsByKeyword("키워드", ReportStatus.PENDING, pageable)).isSameAs(page);
    }

    // ── deleteContentAndResolve ──────────────────────────────────────────

    @Test
    void 신고_승인시_대상_유저_탈퇴처리하고_해당_유저의_대기신고_전부_해결됨() {
        User target = user(10L);
        User reporter = user(20L);
        UserReport r1 = report(1L, target, reporter);
        UserReport r2 = report(2L, target, user(21L));
        given(reportRepository.findById(1L)).willReturn(Optional.of(r1));
        given(reportRepository.findByTargetId(10L)).willReturn(List.of(r1, r2));

        service.deleteContentAndResolve(1L);

        verify(userAdminService).adminDeleteUser(10L);
        assertThat(r1.isPending()).isFalse();
        assertThat(r2.isPending()).isFalse();
    }

    @Test
    void 존재하지_않는_신고_승인시_예외() {
        given(reportRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteContentAndResolve(1L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // 같은 유저를 대상으로 한 신고 2건을 벌크 삭제(순차 deleteContentAndResolve 호출)하면
    // 첫 건 처리 때 두 신고 모두 USER_DELETED로 정리되므로, 두 번째 처리 시점엔 이미
    // isPending()이 false라 adminDeleteUser가 다시 호출되면 안 된다.
    @Test
    void 같은_유저_대상_신고_두건_순차처리시_탈퇴처리는_한번만_실행() {
        User target = user(10L);
        UserReport r1 = report(1L, target, user(20L));
        UserReport r2 = report(2L, target, user(21L));
        given(reportRepository.findById(1L)).willReturn(Optional.of(r1));
        given(reportRepository.findById(2L)).willReturn(Optional.of(r2));
        given(reportRepository.findByTargetId(10L)).willReturn(List.of(r1, r2));

        service.deleteContentAndResolve(1L);
        service.deleteContentAndResolve(2L);

        verify(userAdminService).adminDeleteUser(10L);
        assertThat(r1.isPending()).isFalse();
        assertThat(r2.isPending()).isFalse();
    }

    // ── dismissReport / bulkDismiss ──────────────────────────────────────

    @Test
    void 신고_기각시_상태가_REJECTED로_변경() {
        UserReport r = report(1L, user(10L), user(20L));
        given(reportRepository.findById(1L)).willReturn(Optional.of(r));

        service.dismissReport(1L);

        assertThat(r.isPending()).isFalse();
    }

    @Test
    void 일괄_기각시_대기중인_신고만_REJECTED로_변경() {
        UserReport pending = report(1L, user(10L), user(20L));
        UserReport resolved = report(2L, user(11L), user(21L));
        resolved.resolve(ReportStatus.REJECTED);
        given(reportRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(pending, resolved));

        service.bulkDismiss(List.of(1L, 2L));

        assertThat(pending.isPending()).isFalse();
    }

    @Test
    void 일괄_기각_ID목록이_비어있으면_조회_생략() {
        service.bulkDismiss(List.of());

        verify(reportRepository, never()).findAllById(any());
    }

    // ── 부가 메서드 ──────────────────────────────────────────────────────

    @Test
    void extractAuthorId는_신고_대상_ID_반환() {
        UserReport r = report(1L, user(10L), user(20L));

        assertThat(service.extractAuthorId(r)).isEqualTo(10L);
    }

    @Test
    void getAuthorReportCounts는_유저ID가_비어있으면_빈맵() {
        assertThat(service.getAuthorReportCounts(List.of())).isEmpty();
        verify(reportRepository, never()).countByTargetIds(any());
    }

    @Test
    void getAuthorReportCounts는_유저별_신고건수_맵_반환() {
        given(reportRepository.countByTargetIds(List.of(10L)))
                .willReturn(List.<Object[]>of(new Object[]{10L, 3L}));

        Map<Long, Long> result = service.getAuthorReportCounts(List.of(10L));

        assertThat(result).containsEntry(10L, 3L);
    }

    @Test
    void getReportCountForUser는_단건_카운트_반환() {
        given(reportRepository.countByTargetIds(List.of(10L)))
                .willReturn(List.<Object[]>of(new Object[]{10L, 5L}));

        assertThat(service.getReportCountForUser(10L)).isEqualTo(5L);
    }
}
