package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.ContentTrendDto;
import com.feple.feple_backend.admin.DailyStatDto;
import com.feple.feple_backend.admin.UserActivityStatsDto;
import com.feple.feple_backend.user.entity.User;

import java.time.LocalDate;
import java.util.List;

public interface AdminMetricsService {
    long getTotalUserCount();
    List<User> getRecentUsers();
    List<DailyStatDto> getDailyStats();
    List<DailyStatDto> getMonthlyStats();
    List<DailyStatDto> getRangeStats(LocalDate from, LocalDate to);
    UserActivityStatsDto getUserActivityStats();
    ContentTrendDto getContentTrend();
}
