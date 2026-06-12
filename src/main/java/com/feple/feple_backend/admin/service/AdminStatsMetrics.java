package com.feple.feple_backend.admin.service;

import com.feple.feple_backend.admin.ContentTrendDto;
import com.feple.feple_backend.admin.DailyStatDto;
import com.feple.feple_backend.admin.UserActivityStatsDto;

import java.time.LocalDate;
import java.util.List;

public interface AdminStatsMetrics {
    List<DailyStatDto> getMonthlyStats();
    List<DailyStatDto> getRangeStats(LocalDate from, LocalDate to);
    UserActivityStatsDto getUserActivityStats();
    ContentTrendDto getContentTrend();
}
