package com.feple.feple_backend.festival.dto;

import com.feple.feple_backend.festival.entity.Festival;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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

    public static FestivalResponseDto from(Festival festival) {
        return FestivalResponseDto.builder()
                .id(festival.getId())
                .title(festival.getTitle())
                .description(festival.getDescription())
                .location(festival.getLocation())
                .startDate(festival.getStartDate())
                .endDate(festival.getEndDate())
                .posterUrl(festival.getPosterUrl())
                .build();
    }
}