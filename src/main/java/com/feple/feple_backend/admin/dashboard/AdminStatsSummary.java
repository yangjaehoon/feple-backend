package com.feple.feple_backend.admin.dashboard;

public record AdminStatsSummary(
        long totalFestivals,
        long totalArtists,
        long totalPosts,
        long totalUsers,
        long recentPostCount
) {}
