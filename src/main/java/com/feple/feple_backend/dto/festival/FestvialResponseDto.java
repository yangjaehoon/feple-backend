package com.feple.feple_backend.dto.festival;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class FestvialResponseDto {
    private Long id;
    private String title;
    private String description;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String posterUrl;
    private List<ArtistDto> artists;

    @Getter
    @Builder
    public static class ArtistDto {
        private Long id;
        private String name;
        private String genre;
        private String profileImageUrl;

    }
}
