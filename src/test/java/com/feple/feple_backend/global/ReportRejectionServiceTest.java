package com.feple.feple_backend.global;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.global.entity.ResolvableReport;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.ListCrudRepository;

class ReportRejectionServiceTest {

    interface FakeReportRepository extends ListCrudRepository<ResolvableReport, Long> {}

    @Test
    void reject_대기중_신고면_REJECTED로_전환() {
        FakeReportRepository repo = mock(FakeReportRepository.class);
        ResolvableReport report = mock(ResolvableReport.class);
        given(repo.findById(1L)).willReturn(Optional.of(report));
        given(report.isPending()).willReturn(true);

        ReportRejectionService.reject(repo, 1L);

        verify(report).resolve(ReportStatus.REJECTED);
    }

    @Test
    void reject_이미_처리된_신고면_예외() {
        FakeReportRepository repo = mock(FakeReportRepository.class);
        ResolvableReport report = mock(ResolvableReport.class);
        given(repo.findById(1L)).willReturn(Optional.of(report));
        given(report.isPending()).willReturn(false);

        assertThatThrownBy(() -> ReportRejectionService.reject(repo, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 처리된 신고입니다.");
        verify(report, never()).resolve(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void bulkDismiss_빈_목록이면_조회_생략() {
        FakeReportRepository repo = mock(FakeReportRepository.class);

        ReportRejectionService.bulkDismiss(repo, List.of());

        verify(repo, never()).findAllById(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void bulkDismiss_대기중인_것만_REJECTED로_전환() {
        FakeReportRepository repo = mock(FakeReportRepository.class);
        ResolvableReport pending = mock(ResolvableReport.class);
        ResolvableReport alreadyResolved = mock(ResolvableReport.class);
        given(pending.isPending()).willReturn(true);
        given(alreadyResolved.isPending()).willReturn(false);
        given(repo.findAllById(List.of(1L, 2L))).willReturn(List.of(pending, alreadyResolved));

        ReportRejectionService.bulkDismiss(repo, List.of(1L, 2L));

        verify(pending).resolve(ReportStatus.REJECTED);
        verify(alreadyResolved, never()).resolve(org.mockito.ArgumentMatchers.any());
    }
}
