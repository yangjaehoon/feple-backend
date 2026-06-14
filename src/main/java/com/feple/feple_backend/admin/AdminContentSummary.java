package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.post.dto.PostResponseDto;

import java.util.List;

public record AdminContentSummary(
        List<PostResponseDto> hotPosts,
        List<ArtistResponseDto> topArtists,
        List<UserSummaryDto> recentUsers,
        List<DailyStatDto> dailyStats
) {}
