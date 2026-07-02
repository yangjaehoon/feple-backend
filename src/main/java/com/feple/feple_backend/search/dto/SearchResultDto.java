package com.feple.feple_backend.search.dto;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.post.dto.PostResponseDto;

import java.util.List;

public record SearchResultDto(
        List<ArtistResponseDto> artists,
        List<FestivalResponseDto> festivals,
        List<PostResponseDto> posts
) {}
