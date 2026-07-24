package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.dashboard.ContentTrendDto;
import com.feple.feple_backend.admin.dashboard.DailyStatDto;
import com.feple.feple_backend.admin.user.UserActivityStatsDto;

import java.time.LocalDate;
import java.util.List;

public interface AdminStatsMetrics {
    List<DailyStatDto> getRangeStats(LocalDate from, LocalDate to);
    UserActivityStatsDto getUserActivityStats();
    ContentTrendDto getContentTrend();
}
