package com.feple.feple_backend.artist.song.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SongFestivalDto {
    private Long festivalId;
    private String festivalTitle;
    private String startDate;
}
