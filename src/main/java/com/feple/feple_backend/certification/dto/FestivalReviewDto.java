package com.feple.feple_backend.certification.dto;

import com.feple.feple_backend.certification.entity.FestivalCertification;

import java.time.format.DateTimeFormatter;

public record FestivalReviewDto(
        String nickname,
        Integer rating,
        String userReview,
        String ratedAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static FestivalReviewDto from(FestivalCertification cert) {
        return new FestivalReviewDto(
                cert.getUserNickname(),
                cert.getRating(),
                cert.getUserReview(),
                cert.getRatedAt() != null ? cert.getRatedAt().format(FORMATTER) : null
        );
    }
}
