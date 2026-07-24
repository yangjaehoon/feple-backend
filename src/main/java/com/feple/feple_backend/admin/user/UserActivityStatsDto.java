package com.feple.feple_backend.admin.user;

public record UserActivityStatsDto(
        long dau,
        long wau,
        long mau,
        long signupsToday,
        long signupsThisWeek,
        long signupsThisMonth
) {}
