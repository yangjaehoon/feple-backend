package com.feple.feple_backend.artistfestival.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArtistFestivalResponse {
    private Long artistFestivalId;

    private Long artistId;
    private String artistName;
    private String artistGenre;
    private String profileImageUrl;

    private Integer lineupOrder;
    private String stageName;

}
