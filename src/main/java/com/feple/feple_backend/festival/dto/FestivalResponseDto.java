package com.feple.feple_backend.festival.dto;

import com.feple.feple_backend.festival.entity.AgeRestriction;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.entity.FestivalPeriod;
import com.feple.feple_backend.festival.entity.Region;
import com.feple.feple_backend.global.MusicGenre;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    // 개최 상태는 "오늘"에 의존하는 파생값 — DTO가 시계를 직접 참조하지 않도록 from()에서 계산해 담는다
    private boolean ended;
    private boolean ongoing;
    private boolean upcoming;

    public String getStartDateIso() { return startDate != null ? startDate.toString() : null; }
    public String getEndDateIso()   { return endDate   != null ? endDate.toString()   : null; }

    public String getDateRangeDisplay() {
        if (startDate == null) return "";
        DateTimeFormatter full = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        if (endDate == null || startDate.equals(endDate))
            return startDate.format(full);
        return startDate.format(full) + " ~ " + endDate.format(DateTimeFormatter.ofPattern("MM.dd"));
    }

    public static FestivalResponseDto from(Festival festival, String posterUrl, LocalDate today) {
        FestivalCoreFields.Values core = FestivalCoreFields.of(festival, posterUrl);
        return FestivalResponseDto.builder()
                .ended(FestivalPeriod.isEnded(core.endDate(), today))
                .ongoing(FestivalPeriod.isOngoing(core.startDate(), core.endDate(), today))
                .upcoming(FestivalPeriod.isUpcoming(core.startDate(), today))
                .id(core.id())
                .title(core.title())
                .titleEn(core.titleEn())
                .description(core.description())
                .location(core.location())
                .startDate(core.startDate())
                .endDate(core.endDate())
                .posterUrl(core.posterUrl())
                .likeCount(festival.getLikeCount())
                .attendingCount(core.attendingCount())
                .genres(core.genres())
                .region(festival.getRegion())
                .ageRestriction(core.ageRestriction())
                .latitude(core.latitude())
                .longitude(core.longitude())
                .build();
    }
}
