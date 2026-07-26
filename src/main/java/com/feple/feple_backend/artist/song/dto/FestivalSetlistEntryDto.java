package com.feple.feple_backend.artist.song.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

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
