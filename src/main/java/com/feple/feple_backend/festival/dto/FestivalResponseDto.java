package com.feple.feple_backend.festival.dto;

import com.feple.feple_backend.festival.entity.AgeRestriction;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.global.MusicGenre;
import com.feple.feple_backend.festival.entity.Region;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.time.LocalDate.now;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FestivalResponseDto {

    private Long id;
    private String title;
    private String titleEn;
    private String description;
    private String location;
    private LocalDate startDate;
    private LocalDate endDate;
    private String posterUrl;
    private int likeCount;
    private int attendingCount;
    private List<MusicGenre> genres;
    private Region region;
    private AgeRestriction ageRestriction;
    private Double latitude;
    private Double longitude;

    public boolean isEnded() {
        return endDate != null && endDate.isBefore(now());
    }

    public boolean isOngoing() {
        LocalDate today = now();
        return (endDate == null || !endDate.isBefore(today))
                && startDate != null && !startDate.isAfter(today);
    }

    public boolean isUpcoming() {
        return startDate != null && startDate.isAfter(now());
    }

    public String getStartDateIso() { return startDate != null ? startDate.toString() : null; }
    public String getEndDateIso()   { return endDate   != null ? endDate.toString()   : null; }

    public String getDateRangeDisplay() {
        if (startDate == null) return "";
        DateTimeFormatter full = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        if (endDate == null || startDate.equals(endDate))
            return startDate.format(full);
        return startDate.format(full) + " ~ " + endDate.format(DateTimeFormatter.ofPattern("MM.dd"));
    }

    public static FestivalResponseDto from(Festival festival, String posterUrl) {
        return FestivalResponseDto.builder()
                .id(festival.getId())
                .title(festival.getTitle())
                .titleEn(festival.getTitleEn())
                .description(festival.getDescription())
                .location(festival.getLocation())
                .startDate(festival.getStartDate())
                .endDate(festival.getEndDate())
                .posterUrl(posterUrl)
                .likeCount(festival.getLikeCount())
                .attendingCount(festival.getAttendingCount())
                .genres(festival.getGenres() == null ? List.of() : List.copyOf(festival.getGenres()))
                .region(festival.getRegion())
                .ageRestriction(festival.getAgeRestriction())
                .latitude(festival.getLatitude())
                .longitude(festival.getLongitude())
                .build();
    }
}