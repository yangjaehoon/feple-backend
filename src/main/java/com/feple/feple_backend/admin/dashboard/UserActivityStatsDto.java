package com.feple.feple_backend.admin.dashboard;

public record UserActivityStatsDto(
        long dau,
        long wau,
        long mau,
        long signupsToday,
        long signupsThisWeek,
        long signupsThisMonth,
        long visitorsToday,
        long visitorsThisWeek,
        long visitorsThisMonth
) {}
