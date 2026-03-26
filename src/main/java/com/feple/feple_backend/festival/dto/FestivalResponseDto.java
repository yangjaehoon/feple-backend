package com.feple.feple_backend.festival.dto;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FestivalResponseDto {

    private Long id;
    private String title;
    private String description;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String posterUrl;
    private int likeCount;
    private List<Genre> genres;
    private Region region;

    public static FestivalResponseDto from(Festival festival) {
        return from(festival, festival.getPosterKey());
    }

    public static FestivalResponseDto from(Festival festival, String posterUrl) {
        return FestivalResponseDto.builder()
                .id(festival.getId())
                .title(festival.getTitle())
                .description(festival.getDescription())
                .location(festival.getLocation())
                .startDate(festival.getStartDate())
                .endDate(festival.getEndDate())
                .posterUrl(posterUrl)
                .likeCount(festival.getLikeCount())
                .genres(festival.getGenres())
                .region(festival.getRegion())
                .build();
    }
}