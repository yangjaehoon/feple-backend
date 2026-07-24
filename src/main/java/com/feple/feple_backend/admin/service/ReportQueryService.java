package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.global.JpqlLikeEscaper;
import com.feple.feple_backend.post.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        Set<Long> ids = reports.getContent().stream()
                .map(this::extractAuthorId).collect(Collectors.toSet());
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
