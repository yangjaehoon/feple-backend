package com.feple.feple_backend.festival.dto;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.festival.domain.Festival;
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
    private String description;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String posterUrl;
    private List<ArtistResponseDto> artists;

    public static FestivalDetailResponseDto from(Festival festival) {
        return FestivalDetailResponseDto.builder()
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
