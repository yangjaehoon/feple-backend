package com.feple.feple_backend.admin.service;

import org.springframework.data.domain.Page;

import java.util.Map;

public interface ReportAdminService {
    Page<?> getReportsForAdmin(int page, int size, String statusFilter);
    long getPendingCount();
    long getTotalCount();
    void dismissReport(Long reportId);
    Map<Long, Long> buildAuthorReportCounts(Page<?> reports);
}
