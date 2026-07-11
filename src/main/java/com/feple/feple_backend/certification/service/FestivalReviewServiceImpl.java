package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.certification.dto.FestivalReviewDto;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.entity.CertificationReviewLike;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.certification.repository.CertificationReviewLikeRepository;
import com.feple.feple_backend.global.EntityLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FestivalReviewServiceImpl implements FestivalReviewService {

    private final FestivalCertificationRepository certificationRepository;
    private final CertificationReviewLikeRepository reviewLikeRepository;

    @Override
    @Transactional
    public void submitRating(Long userId, Long certId, int rating, String review) {
        FestivalCertification cert = EntityLoader.getOrThrow(certificationRepository::findById, certId, "인증");
        if (!cert.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 인증에만 평점을 남길 수 있습니다.");
        }
        if (!cert.isApproved()) {
            throw new IllegalArgumentException("승인된 인증에만 평점을 남길 수 있습니다.");
        }
        cert.rate(rating, review);
    }

    @Override
    @Transactional(readOnly = true)
    public double getAverageRating(Long festivalId) {
        Double avg = certificationRepository.getAverageRatingByFestivalId(festivalId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Override
    @Transactional(readOnly = true)
    public int getRatingCount(Long festivalId) {
        return certificationRepository.getRatingCountByFestivalId(festivalId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Long> getRatingDistribution(Long festivalId) {
        Map<Integer, Long> dist = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) dist.put(star, 0L);
        certificationRepository.getRatingDistributionByFestivalId(festivalId)
                .forEach(row -> dist.put(((Number) row[0]).intValue(), (Long) row[1]));
        return dist;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getFestivalReviewsPage(Long festivalId, int page, Long userId) {
        Map<Integer, Long> distribution = getRatingDistribution(festivalId);
        long ratingCount = distribution.values().stream().mapToLong(Long::longValue).sum();
        double averageRating = ratingCount > 0 ? getAverageRating(festivalId) : 0.0;

        Page<FestivalCertification> certPage =
                certificationRepository.findReviewsByFestivalId(festivalId, PageRequest.of(page, 10));

        List<Long> certIds = certPage.getContent().stream().map(FestivalCertification::getId).toList();
        Set<Long> likedCertIds = (userId != null && !certIds.isEmpty())
                ? reviewLikeRepository.findLikedCertIdsByUserIdIn(userId, new HashSet<>(certIds))
                : Set.of();

        List<FestivalReviewDto> reviews = certPage.getContent().stream()
                .map(cert -> FestivalReviewDto.from(cert, likedCertIds.contains(cert.getId())))
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("averageRating", averageRating);
        result.put("ratingCount", (int) ratingCount);
        result.put("distribution", distribution);
        result.put("reviews", reviews);
        result.put("totalPages", certPage.getTotalPages());
        result.put("hasNext", !certPage.isLast());
        return result;
    }

    @Override
    @Transactional
    public boolean toggleReviewLike(Long userId, Long certId) {
        FestivalCertification cert = EntityLoader.getOrThrow(certificationRepository::findById, certId, "리뷰");
        if (!cert.isApproved() || cert.getRating() == null) {
            throw new IllegalArgumentException("존재하지 않는 리뷰입니다.");
        }
        int deleted = reviewLikeRepository.deleteByUserIdAndCertificationId(userId, certId);
        if (deleted > 0) {
            certificationRepository.decrementLikeCount(certId);
            return false;
        }
        reviewLikeRepository.save(CertificationReviewLike.of(userId, certId));
        certificationRepository.incrementLikeCount(certId);
        return true;
    }

    @Override
    @Transactional
    public void removeReviewLikesByUser(Long userId) {
        reviewLikeRepository.deleteByUserId(userId);
        reviewLikeRepository.deleteByCertificationUserId(userId);
    }
}
