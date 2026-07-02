package com.feple.feple_backend.artist.song.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FestivalSetlistEntryDto {
    private Long artistFestivalId;
    private Long artistId;
    private String artistName;
    private String artistNameEn;
    private String profileImageUrl;
    private List<SongResponseDto> songs;
}
