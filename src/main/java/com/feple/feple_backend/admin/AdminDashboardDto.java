package com.feple.feple_backend.admin;

public record AdminDashboardDto(
        AdminStatsSummary stats,
        AdminPendingItemsSummary pending,
        AdminContentSummary content
) {}
