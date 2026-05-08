package com.feple.feple_backend.admin.service;

import org.springframework.data.domain.Page;

public interface ReportAdminService {
    Page<?> getReportsForAdmin(int page, int size, String statusFilter);
    long getPendingCount();
    long getTotalCount();
}
