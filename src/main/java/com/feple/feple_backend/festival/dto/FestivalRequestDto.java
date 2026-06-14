package com.feple.feple_backend.festival.dto;

import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.festival.entity.AgeRestriction;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "페스티벌 제목은 필수입니다.")
    @Size(max = 200, message = "페스티벌 제목은 200자 이하로 입력해주세요.")
    private String title;

    private String titleEn;

    @NotBlank(message = "페스티벌 설명은 필수입니다.")
    @Size(max = 1000, message = "페스티벌 설명은 1000자 이하로 입력해주세요.")
    private String description;
    @NotBlank(message = "페스티벌 장소는 필수입니다.")
    @Size(max = 255, message = "페스티벌 장소는 255자 이하로 입력해주세요.")
    private String location;
    @NotNull(message = "시작일은 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    private String posterKey;

    private List<ArtistRequestDto> artists;

    private List<Genre> genres;
    private Region region;
    private AgeRestriction ageRestriction;
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private Double latitude;
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private Double longitude;

    public static FestivalRequestDto from(FestivalResponseDto src) {
        return FestivalRequestDto.builder()
                .title(src.getTitle())
                .titleEn(src.getTitleEn())
                .description(src.getDescription())
                .location(src.getLocation())
                .startDate(src.getStartDate())
                .endDate(src.getEndDate())
                .region(src.getRegion())
                .ageRestriction(src.getAgeRestriction())
                .genres(src.getGenres())
                .latitude(src.getLatitude())
                .longitude(src.getLongitude())
                .build();
    }
}
