package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.dashboard.DailyStatDto;
import com.feple.feple_backend.admin.user.UserSummaryDto;

import java.util.List;

public interface AdminDashboardMetrics {
    long getTotalUserCount();
    List<UserSummaryDto> getRecentUsers();
    List<DailyStatDto> getDailyStats();
}
