package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.artist.service.S3PresignService;
import com.feple.feple_backend.certification.dto.CertificationResponseDto;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        String prefix = "certifications/" + userId + "/";
        if (photoKey == null || !photoKey.startsWith(prefix)) {
            throw new IllegalArgumentException("잘못된 오브젝트 키입니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Festival festival = festivalRepository.findById(festivalId)
                .orElseThrow(() -> new IllegalArgumentException("페스티벌을 찾을 수 없습니다."));

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
                    String posterUrl = cert.getFestival().getPosterKey() != null
                            ? s3PresignService.presignGetUrl(cert.getFestival().getPosterKey())
                            : null;
                    String photoUrl = s3PresignService.presignGetUrl(cert.getPhotoKey());
                    return CertificationResponseDto.from(cert, posterUrl, photoUrl);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> getApprovedFestivalIds(Long userId) {
        return certificationRepository.findByUserIdAndStatus(userId, CertificationStatus.APPROVED).stream()
                .map(cert -> cert.getFestival().getId())
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<FestivalCertification> getByStatus(CertificationStatus status, Pageable pageable) {
        if (status == null) {
            return certificationRepository.findAll(pageable);
        }
        return certificationRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public FestivalCertification getById(Long id) {
        return certificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("인증 신청을 찾을 수 없습니다."));
    }

    @Transactional
    public void approve(Long certId, String reviewerName) {
        FestivalCertification cert = getById(certId);
        cert.approve(reviewerName);
        // 비동기 알림
        notificationService.notifyCertApproved(
                cert.getUser().getId(),
                cert.getFestival().getTitle(),
                cert.getFestival().getId());
    }

    @Transactional
    public void reject(Long certId, String rejectionMessage, String reviewerName) {
        FestivalCertification cert = getById(certId);
        cert.reject(rejectionMessage, reviewerName);
        // 비동기 알림
        notificationService.notifyCertRejected(
                cert.getUser().getId(),
                cert.getFestival().getTitle(),
                cert.getFestival().getId(),
                rejectionMessage);
    }
}
