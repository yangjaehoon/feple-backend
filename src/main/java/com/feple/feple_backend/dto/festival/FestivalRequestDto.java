package com.feple.feple_backend.dto.festival;

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

    private List<ArtistDto> artists;

    @Getter
    public static class ArtistDto {
        private String name;
        private String genre;
        private String profileImageUrl;
    }
}
