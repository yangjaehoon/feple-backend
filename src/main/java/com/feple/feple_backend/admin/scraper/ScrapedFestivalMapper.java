package com.feple.feple_backend.admin.scraper;

import com.feple.feple_backend.festival.dto.FestivalRequestDto;
import com.feple.feple_backend.festival.entity.Genre;
import com.feple.feple_backend.festival.entity.Region;

import java.time.LocalDate;

public final class ScrapedFestivalMapper {

    private ScrapedFestivalMapper() {}

    public static FestivalRequestDto toFestivalRequestDto(ScraperApplyRequestDto req) {
        FestivalRequestDto dto = new FestivalRequestDto();
        dto.setTitle(req.title().trim());
        dto.setTitleEn(req.titleEn() != null ? req.titleEn().trim() : null);
        dto.setDescription(req.description() != null ? req.description().trim() : "");
        dto.setLocation(req.location() != null ? req.location().trim() : "");
        dto.setStartDate(LocalDate.parse(req.startDate()));
        dto.setEndDate(LocalDate.parse(req.endDate()));

        if (req.region() != null && !req.region().isBlank()) {
            try {
                dto.setRegion(Region.valueOf(req.region()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("유효하지 않은 지역 값입니다: " + req.region());
            }
        }
        if (req.genres() != null && !req.genres().isEmpty()) {
            dto.setGenres(req.genres().stream()
                .filter(g -> g != null && !g.isBlank())
                .map(g -> {
                    try { return Genre.valueOf(g); }
                    catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("유효하지 않은 장르 값입니다: " + g);
                    }
                })
                .toList());
        }
        return dto;
    }
}
