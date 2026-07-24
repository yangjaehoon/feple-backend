package com.feple.feple_backend.admin.dashboard;

public record AdminDashboardDto(
        AdminStatsSummary stats,
        AdminPendingItemsSummary pending,
        AdminContentSummary content
) {}
