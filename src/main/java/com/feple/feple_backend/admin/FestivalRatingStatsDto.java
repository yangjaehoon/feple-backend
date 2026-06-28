package com.feple.feple_backend.admin;

import java.util.Map;

public record FestivalRatingStatsDto(
        double averageRating,
        int ratingCount,
        Map<Integer, Long> distribution   // 별점(1~5) → 평가 수
) {
    public static final FestivalRatingStatsDto EMPTY =
            new FestivalRatingStatsDto(0.0, 0, Map.of());
}
