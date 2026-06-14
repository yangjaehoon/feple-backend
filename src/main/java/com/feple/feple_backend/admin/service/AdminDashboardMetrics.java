package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.DailyStatDto;
import com.feple.feple_backend.admin.UserSummaryDto;

import java.util.List;

public interface AdminDashboardMetrics {
    long getTotalUserCount();
    List<UserSummaryDto> getRecentUsers();
    List<DailyStatDto> getDailyStats();
}
