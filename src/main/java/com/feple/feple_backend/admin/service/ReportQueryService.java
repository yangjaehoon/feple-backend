package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.global.JpqlLikeEscaper;
import com.feple.feple_backend.global.entity.ReportStatus;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

public interface ReportQueryService<T> {
    String getReportType();
    Page<T> findPendingReports(PageRequest pageable);
    Page<T> findAllReports(PageRequest pageable);
    Page<T> searchReportsByKeyword(String escapedKeyword, ReportStatus status, PageRequest pageable);
    long getPendingCount();
    long getTotalCount();
    Long extractAuthorId(T report);
    Map<Long, Long> getAuthorReportCounts(Collection<Long> userIds);

    default Map<Long, Long> buildAuthorReportCounts(Page<T> reports) {
        // 대상 글/댓글이 하드 삭제되면 extractAuthorId가 null을 반환한다(PostReport.getPostAuthorId 등).
        // 걸러내지 않으면 IN 절에 NULL이 섞인다 — MySQL에서 매칭되지 않고 끝나지만,
        // null-safe 게터를 둔 취지대로 호출부에서도 제외한다.
        Set<Long> ids = reports.getContent().stream()
                .map(this::extractAuthorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return getAuthorReportCounts(ids);
    }

    default Page<T> getReportsForAdmin(int page, int size, String statusFilter) {
        PageRequest pageable = PageRequest.of(page, size);
        // fromFilter는 "PENDING"에만 non-null 반환. pendingOnly=true면 미처리만, false면 전체.
        boolean pendingOnly = ReportStatus.fromFilter(statusFilter) != null;
        return pendingOnly ? findPendingReports(pageable) : findAllReports(pageable);
    }

    default Page<T> searchReportsForAdmin(ReportSearchParams params) {
        String keyword = JpqlLikeEscaper.escapeOrNull(params.keyword());
        if (keyword == null) return getReportsForAdmin(params.page(), params.size(), params.statusFilter());
        return searchReportsByKeyword(keyword, ReportStatus.fromFilter(params.statusFilter()), PageRequest.of(params.page(), params.size()));
    }
}
