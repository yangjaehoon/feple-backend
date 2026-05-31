package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.song.entity.SongRequest;
import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.user.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;

public record AdminDashboardDto(
        Page<FestivalResponseDto> festivalPage,
        Page<ArtistResponseDto> artistPage,
        long totalPosts,
        long totalUsers,
        long recentPostCount,
        List<PostResponseDto> hotPosts,
        List<ArtistResponseDto> topArtists,
        List<User> recentUsers,
        List<DailyStatDto> dailyStats,
        List<FestivalCertification> pendingCerts,
        long pendingCertCount,
        List<PostReport> pendingReports,
        long pendingReportCount,
        List<SongRequest> pendingSongRequests,
        long pendingSongRequestCount,
        List<ArtistSuggestionResponseDto> pendingArtistSuggestions,
        long pendingArtistSuggestionCount
) {}
