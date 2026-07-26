package com.feple.feple_backend.artistfestival.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArtistFestivalResponseDto {
    private Long artistFestivalId;

    private Long artistId;
    private String artistName;
    private String artistNameEn;
    private String artistGenre;
    private String profileImageUrl;

    private Integer lineupOrder;
    private String stageName;
    private String performanceDate;
    private List<String> performanceDates;

}
