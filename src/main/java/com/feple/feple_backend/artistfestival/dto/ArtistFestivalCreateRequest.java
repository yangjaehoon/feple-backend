package com.feple.feple_backend.artistfestival.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ArtistFestivalCreateRequest {

    private Long artistId;
    private Integer lineupOrder;
    private String stageName;

}
