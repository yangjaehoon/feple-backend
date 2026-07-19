package com.feple.feple_backend.festival.dto;

import com.feple.feple_backend.festival.entity.AgeRestriction;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.global.MusicGenre;

import java.time.LocalDate;
import java.util.List;

/** FestivalResponseDto/FestivalDetailResponseDto가 공통으로 매핑하는 Festival 필드 추출 */
final class FestivalCoreFields {
    private FestivalCoreFields() {}

    record Values(Long id, String title, String titleEn, String description, String location,
                  LocalDate startDate, LocalDate endDate, String posterUrl,
                  Double latitude, Double longitude, List<MusicGenre> genres,
                  AgeRestriction ageRestriction, int attendingCount) {}

    static Values of(Festival festival, String posterUrl) {
        return new Values(
                festival.getId(), festival.getTitle(), festival.getTitleEn(), festival.getDescription(),
                festival.getLocation(), festival.getStartDate(), festival.getEndDate(), posterUrl,
                festival.getLatitude(), festival.getLongitude(),
                festival.getGenres() == null ? List.of() : List.copyOf(festival.getGenres()),
                festival.getAgeRestriction(), festival.getAttendingCount());
    }
}
