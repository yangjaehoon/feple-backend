package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;

import java.util.List;

public record AdminDashboardDto(
        long totalFestivals,
        long totalArtists,
        long totalPosts,
        long totalUsers,
        long recentPostCount,
        List<PostResponseDto> hotPosts,
        List<ArtistResponseDto> topArtists,
        List<UserSummaryDto> recentUsers,
        List<DailyStatDto> dailyStats,
        List<CertSummaryDto> pendingCerts,
        long pendingCertCount,
        List<ReportSummaryDto> pendingReports,
        long pendingReportCount,
        List<SongRequestSummaryDto> pendingSongRequests,
        long pendingSongRequestCount,
        List<ArtistSuggestionResponseDto> pendingArtistSuggestions,
        long pendingArtistSuggestionCount
) {}
