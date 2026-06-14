package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.comment.dto.MyCommentResponseDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.dto.UserStatsDto;

import java.util.List;

public record UserDetailModel(
        UserResponseDto user,
        UserStatsDto stats,
        List<PostResponseDto> recentPosts,
        List<MyCommentResponseDto> recentComments,
        List<FestivalResponseDto> likedFestivals,
        List<ArtistResponseDto> followedArtists
) {}
