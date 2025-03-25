package com.feple.feple_backend.dto.festival;

import com.feple.feple_backend.dto.artist.ArtistResponseDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class FestivalResponseDto {
    private Long id;
    private String title;
    private String description;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String posterUrl;
    private List<ArtistResponseDto> artists;

}
