package com.feple.feple_backend.dto.festival;

import com.feple.feple_backend.dto.artist.ArtistRequestDto;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class FestivalRequestDto {
    private String title;
    private String description;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String posterUrl;

    private List<ArtistRequestDto> artists;

}
