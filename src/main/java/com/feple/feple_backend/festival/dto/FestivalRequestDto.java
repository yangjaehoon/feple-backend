package com.feple.feple_backend.festival.dto;

import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FestivalRequestDto {
    private String title;
    private String description;
    private String location;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    private String posterUrl;

    private List<ArtistRequestDto> artists;

    private List<Genre> genres;
    private Region region;
}
