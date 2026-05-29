package com.feple.feple_backend.admin.service;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

public interface ReportAdminService {
    Page<?> getReportsForAdmin(int page, int size, String statusFilter);
    default Page<?> searchReportsForAdmin(int page, int size, String statusFilter, String keyword) {
        return getReportsForAdmin(page, size, statusFilter);
    }
    long getPendingCount();
    long getTotalCount();
    void dismissReport(Long reportId);
    void bulkDismiss(List<Long> ids);
    Map<Long, Long> buildAuthorReportCounts(Page<?> reports);
}
