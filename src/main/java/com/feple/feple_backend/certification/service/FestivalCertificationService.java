package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.artist.service.S3PresignService;
import com.feple.feple_backend.certification.dto.CertificationResponseDto;
import com.feple.feple_backend.file.S3Keys;
import com.feple.feple_backend.file.dto.PresignResult;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.global.exception.DuplicateArtistFestivalException;
import com.feple.feple_backend.notification.service.NotificationService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FestivalCertificationService {

    private final FestivalCertificationRepository certificationRepository;
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
                    throw new DuplicateArtistFestivalException("이미 해당 페스티벌에 인증 신청을 했습니다.");
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
    public Page<FestivalCertification> getByStatus(CertificationStatus status, int page) {
        PageRequest pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        if (status == null) {
            return certificationRepository.findAll(pageable);
        }
        return certificationRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<FestivalCertification> searchByKeyword(String keyword, CertificationStatus status, int page) {
        PageRequest pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        return certificationRepository.searchByKeyword(keyword, status, pageable);
    }

    @Transactional(readOnly = true)
    public FestivalCertification getById(Long id) {
        return EntityFinder.getOrThrow(certificationRepository::findById, id, "인증 신청");
    }

    @Transactional
    public void approve(Long certId, String reviewerName) {
        FestivalCertification cert = getById(certId);
        cert.approve(reviewerName);
        // 비동기 알림
        notificationService.notifyCertApproved(
                cert.getUserId(),
                cert.getFestivalTitle(),
                cert.getFestivalId());
    }

    @Transactional
    public void reject(Long certId, String rejectionMessage, String reviewerName) {
        FestivalCertification cert = getById(certId);
        cert.reject(rejectionMessage, reviewerName);
        // 비동기 알림
        notificationService.notifyCertRejected(
                cert.getUserId(),
                cert.getFestivalTitle(),
                cert.getFestivalId(),
                rejectionMessage);
    }

    @Transactional(readOnly = true)
    public long getPendingCount() {
        return certificationRepository.countByStatus(CertificationStatus.PENDING);
    }

    public String buildPhotoUrl(String photoKey) {
        return s3PresignService.presignGetUrl(photoKey);
    }

    public PresignResult generateUploadUrl(Long userId, String extension, String contentType) {
        String objectKey = S3Keys.certificationPrefix(userId) + UUID.randomUUID() + "." + extension;
        return s3PresignService.presignPut(objectKey, contentType);
    }
}
