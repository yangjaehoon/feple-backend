package com.feple.feple_backend.admin.service;

import org.springframework.data.domain.Page;

import java.util.Map;

public interface ReportQueryService {
    String getReportType();
    Page<?> getReportsForAdmin(int page, int size, String statusFilter);
    default Page<?> searchReportsForAdmin(int page, int size, String statusFilter, String keyword) {
        return getReportsForAdmin(page, size, statusFilter);
    }
    long getPendingCount();
    long getTotalCount();
    Map<Long, Long> buildAuthorReportCounts(Page<?> reports);
}
