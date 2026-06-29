package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.artist.service.S3PresignService;
import com.feple.feple_backend.certification.dto.CertificationResponseDto;
import com.feple.feple_backend.certification.dto.FestivalReviewDto;
import com.feple.feple_backend.certification.entity.ReviewLike;
import com.feple.feple_backend.certification.repository.ReviewLikeRepository;
import com.feple.feple_backend.file.S3Keys;
import com.feple.feple_backend.file.dto.PresignResult;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.LikeEscaper;
import com.feple.feple_backend.global.PageableFactory;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.notification.service.NotificationService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.feple.feple_backend.global.cache.EvictAdminPendingCaches;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class FestivalCertificationService {

    private final FestivalCertificationRepository certificationRepository;
    private final ReviewLikeRepository reviewLikeRepository;
    private final UserRepository userRepository;
    private final FestivalRepository festivalRepository;
    private final S3PresignService s3PresignService;
    private final NotificationService notificationService;

    @Transactional
    public CertificationResponseDto submit(Long userId, Long festivalId, String photoKey) {
        String prefix = S3Keys.certificationPrefix(userId);
        if (photoKey == null || !photoKey.startsWith(prefix)) {
            throw new IllegalArgumentException("잘못된 오브젝트 키입니다.");
        }

        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        Festival festival = EntityFinder.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");

        certificationRepository.findByUserIdAndFestivalId(userId, festivalId)
                .ifPresent(existing -> {
                    throw new ConflictException("이미 해당 페스티벌에 인증 신청을 했습니다.");
                });

        FestivalCertification cert = FestivalCertification.create(user, festival, photoKey);
        certificationRepository.save(cert);

        String posterUrl = festival.getPosterKey() != null ? s3PresignService.presignGetUrl(festival.getPosterKey()) : null;
        String photoUrl = s3PresignService.presignGetUrl(photoKey);
        return CertificationResponseDto.from(cert, posterUrl, photoUrl);
    }

    @Transactional(readOnly = true)
    public List<CertificationResponseDto> getMyCertifications(Long userId) {
        return certificationRepository.findByUserId(userId).stream()
                .map(cert -> {
                    String posterUrl = cert.getFestivalPosterKey() != null
                            ? s3PresignService.presignGetUrl(cert.getFestivalPosterKey())
                            : null;
                    String photoUrl = s3PresignService.presignGetUrl(cert.getPhotoKey());
                    return CertificationResponseDto.from(cert, posterUrl, photoUrl);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> getApprovedFestivalIds(Long userId) {
        return certificationRepository.findByUserIdAndStatus(userId, CertificationStatus.APPROVED).stream()
                .map(FestivalCertification::getFestivalId)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countApprovedByUser(Long userId) {
        return certificationRepository.countByUserIdAndStatus(userId, CertificationStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public Page<FestivalCertification> getByStatus(CertificationStatus status, int page) {
        Pageable pageable = PageableFactory.latestFirst(page, 20);
        if (status == null) {
            return certificationRepository.findAll(pageable);
        }
        return certificationRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FestivalCertification> searchByKeyword(String keyword, CertificationStatus status, int page) {
        Pageable pageable = PageableFactory.latestFirst(page, 20);
        return certificationRepository.searchByKeyword(LikeEscaper.escape(keyword.trim()), status, pageable);
    }

    @Transactional(readOnly = true)
    public FestivalCertification getById(Long id) {
        return EntityFinder.getOrThrow(certificationRepository::findWithUserAndFestivalById, id, "인증 신청");
    }

    @EvictAdminPendingCaches
    @Transactional
    public void approve(Long certId, String reviewerName) {
        FestivalCertification cert = getById(certId);
        cert.approve(reviewerName);
        // 비동기 알림
        notificationService.notifyCertApproved(
                cert.getUserId(),
                cert.getFestivalTitle(),
                cert.getFestivalTitleEn(),
                cert.getFestivalId());
    }

    @EvictAdminPendingCaches
    @Transactional
    public void reject(Long certId, String rejectionMessage, String reviewerName) {
        FestivalCertification cert = getById(certId);
        cert.reject(rejectionMessage, reviewerName);
        // 비동기 알림
        notificationService.notifyCertRejected(
                cert.getUserId(),
                cert.getFestivalTitle(),
                cert.getFestivalTitleEn(),
                cert.getFestivalId(),
                rejectionMessage);
    }

    @Transactional(readOnly = true)
    public String getCertState(Long userId, Long festivalId) {
        return certificationRepository.findByUserIdAndFestivalId(userId, festivalId)
                .map(cert -> cert.getStatus().name())
                .orElse("NONE");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCertDetail(Long userId, Long festivalId) {
        return certificationRepository.findByUserIdAndFestivalId(userId, festivalId)
                .map(cert -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("certState", cert.getStatus().name());
                    if (cert.isApproved()) {
                        result.put("certId", cert.getId());
                        result.put("myRating", cert.getRating());
                        result.put("myReview", cert.getUserReview());
                    }
                    return result;
                })
                .orElseGet(() -> Map.of("certState", "NONE"));
    }

    @Transactional(readOnly = true)
    public long getPendingCount() {
        return certificationRepository.countByStatus(CertificationStatus.PENDING);
    }

    public String buildPhotoUrl(String photoKey) {
        return s3PresignService.presignGetUrl(photoKey);
    }

    @Transactional
    public void submitRating(Long userId, Long certId, int rating, String review) {
        FestivalCertification cert = EntityFinder.getOrThrow(certificationRepository::findById, certId, "인증");
        if (!cert.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 인증에만 평점을 남길 수 있습니다.");
        }
        if (!cert.isApproved()) {
            throw new IllegalArgumentException("승인된 인증에만 평점을 남길 수 있습니다.");
        }
        cert.rate(rating, review);
    }

    @Transactional(readOnly = true)
    public double getAverageRating(Long festivalId) {
        Double avg = certificationRepository.getAverageRatingByFestivalId(festivalId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Transactional(readOnly = true)
    public int getRatingCount(Long festivalId) {
        return certificationRepository.getRatingCountByFestivalId(festivalId);
    }

    @Transactional(readOnly = true)
    public Map<Integer, Long> getRatingDistribution(Long festivalId) {
        Map<Integer, Long> dist = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) dist.put(star, 0L);
        certificationRepository.getRatingDistributionByFestivalId(festivalId)
                .forEach(row -> dist.put(((Number) row[0]).intValue(), (Long) row[1]));
        return dist;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getFestivalReviewsPage(Long festivalId, int page, Long userId) {
        Map<Integer, Long> distribution = getRatingDistribution(festivalId);
        long ratingCount = distribution.values().stream().mapToLong(Long::longValue).sum();
        double averageRating = ratingCount > 0 ? getAverageRating(festivalId) : 0.0;

        org.springframework.data.domain.Page<FestivalCertification> certPage =
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

    @Transactional
    public boolean toggleReviewLike(Long userId, Long certId) {
        FestivalCertification cert = EntityFinder.getOrThrow(certificationRepository::findById, certId, "리뷰");
        if (!cert.isApproved() || cert.getRating() == null) {
            throw new IllegalArgumentException("존재하지 않는 리뷰입니다.");
        }
        int deleted = reviewLikeRepository.deleteByUserIdAndCertificationId(userId, certId);
        if (deleted > 0) {
            cert.decrementLikeCount();
            return false;
        }
        reviewLikeRepository.save(ReviewLike.of(userId, certId));
        cert.incrementLikeCount();
        return true;
    }

    public void removeReviewLikesByUser(Long userId) {
        reviewLikeRepository.deleteByUserId(userId);
        reviewLikeRepository.deleteByCertificationUserId(userId);
    }

    public PresignResult generateUploadUrl(Long userId, String extension, String contentType) {
        String objectKey = S3Keys.certificationPrefix(userId) + UUID.randomUUID() + "." + extension;
        return s3PresignService.presignPut(objectKey, contentType);
    }
}
