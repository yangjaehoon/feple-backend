package com.feple.feple_backend.festival.entity;

import com.feple.feple_backend.global.MusicGenre;

import java.time.LocalDate;
import java.util.List;

/** Festival.update()에 넘기는 수정 가능 필드 묶음 — 11개 개별 인수 대신 사용 */
public record FestivalUpdateFields(
        String title,
        String titleEn,
        String description,
        String location,
        LocalDate startDate,
        LocalDate endDate,
        List<MusicGenre> genres,
        Region region,
        AgeRestriction ageRestriction,
        Double latitude,
        Double longitude
) {}
