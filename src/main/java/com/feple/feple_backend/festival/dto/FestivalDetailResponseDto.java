package com.feple.feple_backend.festival.dto;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.festival.entity.Festival;
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
//페스티벌 상세 페이지에 들어가는 내용
public class FestivalDetailResponseDto {
    private Long id;
    private String title;
    private String titleEn;
    private String description;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String posterUrl;
    private Double latitude;
    private Double longitude;
    private List<ArtistResponseDto> artists;

    public static FestivalDetailResponseDto from(Festival festival) {
        return from(festival, festival.getPosterKey());
    }

    public static FestivalDetailResponseDto from(Festival festival, String posterUrl) {
        return FestivalDetailResponseDto.builder()
                .id(festival.getId())
                .title(festival.getTitle())
                .titleEn(festival.getTitleEn())
                .description(festival.getDescription())
                .location(festival.getLocation())
                .startDate(festival.getStartDate())
                .endDate(festival.getEndDate())
                .posterUrl(posterUrl)
                .latitude(festival.getLatitude())
                .longitude(festival.getLongitude())
                .build();
    }

}
