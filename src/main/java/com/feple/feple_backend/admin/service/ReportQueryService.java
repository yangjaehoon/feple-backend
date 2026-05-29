package com.feple.feple_backend.admin.service;

import org.springframework.data.domain.Page;

import java.util.Map;

public interface ReportQueryService<T> {
    String getReportType();
    Page<T> getReportsForAdmin(int page, int size, String statusFilter);
    default Page<T> searchReportsForAdmin(ReportSearchParams params) {
        return getReportsForAdmin(params.page(), params.size(), params.statusFilter());
    }
    long getPendingCount();
    long getTotalCount();
    Map<Long, Long> buildAuthorReportCounts(Page<T> reports);
}
