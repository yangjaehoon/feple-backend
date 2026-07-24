package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.certification.dto.CertificationRatingRequestDto;

import java.util.Map;

public interface FestivalReviewService {
    void submitRating(Long userId, Long certId, CertificationRatingRequestDto req);
    double getAverageRating(Long festivalId);
    int getRatingCount(Long festivalId);
    Map<Integer, Long> getRatingDistribution(Long festivalId);
    Map<String, Object> getFestivalReviewsPage(Long festivalId, int page, Long userId);
    boolean toggleReviewLike(Long userId, Long certId);
    void removeReviewLikesByUser(Long userId);
}
