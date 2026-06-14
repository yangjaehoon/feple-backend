package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.global.LikeEscaper;
import com.feple.feple_backend.post.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Map;

public interface ReportQueryService<T> {
    String getReportType();
    Page<T> findPendingReports(PageRequest pageable);
    Page<T> findAllReports(PageRequest pageable);
    Page<T> searchReportsByKeyword(String escapedKeyword, ReportStatus status, PageRequest pageable);
    long getPendingCount();
    long getTotalCount();
    Map<Long, Long> buildAuthorReportCounts(Page<T> reports);

    default Page<T> getReportsForAdmin(int page, int size, String statusFilter) {
        PageRequest pageable = PageRequest.of(page, size);
        return ReportStatus.fromFilter(statusFilter) != null
                ? findPendingReports(pageable)
                : findAllReports(pageable);
    }

    default Page<T> searchReportsForAdmin(ReportSearchParams params) {
        String keyword = LikeEscaper.escapeOrNull(params.keyword());
        if (keyword == null) return getReportsForAdmin(params.page(), params.size(), params.statusFilter());
        return searchReportsByKeyword(keyword, ReportStatus.fromFilter(params.statusFilter()), PageRequest.of(params.page(), params.size()));
    }
}
