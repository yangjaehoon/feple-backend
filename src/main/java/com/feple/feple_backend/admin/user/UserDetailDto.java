package com.feple.feple_backend.admin.user;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.comment.dto.MyCommentResponseDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.dto.UserStatsDto;
import com.feple.feple_backend.userblock.dto.BlockedUserDto;

import java.util.List;

public record UserDetailDto(
        UserResponseDto user,
        UserStatsDto stats,
        List<PostResponseDto> recentPosts,
        List<MyCommentResponseDto> recentComments,
        List<FestivalResponseDto> likedFestivals,
        List<ArtistResponseDto> followedArtists,
        List<BlockedUserDto> blockedUsers
) {}
