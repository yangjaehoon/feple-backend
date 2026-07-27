package com.feple.feple_backend.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.global.entity.ReportStatus;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

class ReportQueryServiceTest {

    @SuppressWarnings("unchecked")
    private final ReportQueryService<String> service = mock(ReportQueryService.class, CALLS_REAL_METHODS);

    // ── getReportsForAdmin ──────────────────────────────────────────────

    @Test
    void statusFilter가_PENDING이면_대기중_신고만_조회한다() {
        Page<String> page = new PageImpl<>(List.of("pending"));
        given(service.findPendingReports(any())).willReturn(page);

        Page<String> result = service.getReportsForAdmin(0, 10, "PENDING");

        assertThat(result).isSameAs(page);
        verify(service).findPendingReports(any());
    }

    @Test
    void statusFilter가_PENDING이_아니면_전체_신고를_조회한다() {
        Page<String> page = new PageImpl<>(List.of("all"));
        given(service.findAllReports(any())).willReturn(page);

        Page<String> result = service.getReportsForAdmin(0, 10, "ALL");

        assertThat(result).isSameAs(page);
        verify(service).findAllReports(any());
    }

    // ── searchReportsForAdmin ───────────────────────────────────────────

    @Test
    void 키워드가_없으면_상태필터_기준_조회로_위임한다() {
        Page<String> page = new PageImpl<>(List.of("no-keyword"));
        given(service.findAllReports(any())).willReturn(page);
        ReportSearchParams params = new ReportSearchParams(0, 10, "ALL", null);

        Page<String> result = service.searchReportsForAdmin(params);

        assertThat(result).isSameAs(page);
    }

    @Test
    void 키워드가_있으면_키워드_검색으로_위임한다() {
        Page<String> page = new PageImpl<>(List.of("검색결과"));
        given(service.searchReportsByKeyword(any(), any(), any())).willReturn(page);
        ReportSearchParams params = new ReportSearchParams(0, 10, "PENDING", "닉네임");

        Page<String> result = service.searchReportsForAdmin(params);

        assertThat(result).isSameAs(page);
        verify(service).searchReportsByKeyword(any(), org.mockito.ArgumentMatchers.eq(ReportStatus.PENDING), any());
    }

    // ── buildAuthorReportCounts ─────────────────────────────────────────

    @Test
    void 페이지_내용에서_작성자_ID를_추출해_신고_카운트를_조회한다() {
        Page<String> reports = new PageImpl<>(List.of("a", "b"));
        given(service.extractAuthorId("a")).willReturn(1L);
        given(service.extractAuthorId("b")).willReturn(2L);
        Map<Long, Long> counts = Map.of(1L, 3L, 2L, 1L);
        given(service.getAuthorReportCounts(Set.of(1L, 2L))).willReturn(counts);

        Map<Long, Long> result = service.buildAuthorReportCounts(reports);

        assertThat(result).isEqualTo(counts);
    }
}
