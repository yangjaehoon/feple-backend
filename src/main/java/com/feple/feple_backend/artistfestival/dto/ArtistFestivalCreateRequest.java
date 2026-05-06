package com.feple.feple_backend.artistfestival.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ArtistFestivalCreateRequest {

    @NotNull(message = "아티스트 ID는 필수입니다.")
    @Positive(message = "아티스트 ID는 양수여야 합니다.")
    private Long artistId;
    private Integer lineupOrder;
    private String stageName;

}
