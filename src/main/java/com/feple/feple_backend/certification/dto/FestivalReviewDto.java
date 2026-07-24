package com.feple.feple_backend.certification.dto;

import com.feple.feple_backend.certification.entity.FestivalCertification;

import java.time.format.DateTimeFormatter;

public record FestivalReviewDto(
        Long reviewId,
        String nickname,
        Integer rating,
        String userReview,
        String ratedAt,
        int likeCount,
        boolean likedByMe
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static FestivalReviewDto from(FestivalCertification cert, boolean likedByMe) {
        return new FestivalReviewDto(
                cert.getId(),
                cert.getUserNickname(),
                cert.getRating(),
                cert.getUserReview(),
                cert.getRatedAt() != null ? cert.getRatedAt().format(FORMATTER) : null,
                cert.getLikeCount(),
                likedByMe
        );
    }
}
