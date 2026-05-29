package com.feple.feple_backend.admin;

import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;

import java.time.LocalDate;

final class ScrapedFestivalMapper {

    private ScrapedFestivalMapper() {}

    static FestivalRequestDto toFestivalRequestDto(ScraperApplyRequest req) {
        FestivalRequestDto dto = new FestivalRequestDto();
        dto.setTitle(req.title().trim());
        dto.setTitleEn(req.titleEn() != null ? req.titleEn().trim() : null);
        dto.setDescription(req.description() != null ? req.description().trim() : "");
        dto.setLocation(req.location() != null ? req.location().trim() : "");
        dto.setStartDate(LocalDate.parse(req.startDate()));
        dto.setEndDate(LocalDate.parse(req.endDate()));

        if (req.region() != null && !req.region().isBlank()) {
            dto.setRegion(Region.valueOf(req.region()));
        }
        if (req.genres() != null && !req.genres().isEmpty()) {
            dto.setGenres(req.genres().stream()
                .filter(g -> g != null && !g.isBlank())
                .map(Genre::valueOf)
                .toList());
        }
        return dto;
    }
}
