package com.feple.feple_backend.festival.dto;

import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private String title;

    private String titleEn;

    @NotBlank(message = "페스티벌 설명은 필수입니다.")
    private String description;
    @NotBlank(message = "페스티벌 장소는 필수입니다.")
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
    private Double latitude;
    private Double longitude;
}
