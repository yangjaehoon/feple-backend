package com.feple.feple_backend.artistfestival.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ArtistFestivalCreateRequest {

    private Long artistId;
    private Integer lineupOrder;
    private String stageName;

}
