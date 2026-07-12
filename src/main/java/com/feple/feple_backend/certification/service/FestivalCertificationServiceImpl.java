package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.certification.dto.CertificationResponseDto;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.S3PathConstants;
import com.feple.feple_backend.file.dto.S3PresignedUrlResult;
import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FestivalCertificationServiceImpl implements FestivalCertificationService {

    private final FestivalCertificationRepository certificationRepository;
    private final UserRepository userRepository;
    private final FestivalRepository festivalRepository;
    private final S3PresignService s3PresignService;

    @Override
    @Transactional
    public CertificationResponseDto submit(Long userId, Long festivalId, String photoKey) {
        String prefix = S3PathConstants.certificationPrefix(userId);
        if (photoKey == null || !photoKey.startsWith(prefix)) {
            throw new IllegalArgumentException("잘못된 오브젝트 키입니다.");
        }

        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");

        certificationRepository.findByUserIdAndFestivalId(userId, festivalId)
                .ifPresent(existing -> {
                    throw new ConflictException("이미 해당 페스티벌에 인증 신청을 했습니다.");
                });

        FestivalCertification cert = FestivalCertification.create(user, festival, photoKey);
        try {
            certificationRepository.saveAndFlush(cert);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("이미 해당 페스티벌에 인증 신청을 했습니다.");
        }

        String posterUrl = festival.getPosterKey() != null ? s3PresignService.presignGetUrl(festival.getPosterKey()) : null;
        String photoUrl = s3PresignService.presignGetUrl(photoKey);
        return CertificationResponseDto.from(cert, posterUrl, photoUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificationResponseDto> getMyCertifications(Long userId) {
        return certificationRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    private CertificationResponseDto toDto(FestivalCertification cert) {
        String posterUrl = cert.getFestivalPosterKey() != null
                ? s3PresignService.presignGetUrl(cert.getFestivalPosterKey())
                : null;
        String photoUrl = s3PresignService.presignGetUrl(cert.getPhotoKey());
        return CertificationResponseDto.from(cert, posterUrl, photoUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getApprovedFestivalIds(Long userId) {
        return certificationRepository.findByUserIdAndStatus(userId, CertificationStatus.APPROVED).stream()
                .map(FestivalCertification::getFestivalId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countApprovedByUser(Long userId) {
        return certificationRepository.countByUserIdAndStatus(userId, CertificationStatus.APPROVED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CertificationResponseDto> getPublicCertifications(Long userId) {
        return certificationRepository.findByUserIdAndStatus(userId, CertificationStatus.APPROVED).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCertDetail(Long userId, Long festivalId) {
        return certificationRepository.findByUserIdAndFestivalId(userId, festivalId)
                .map(cert -> {
                    Map<String, Object> result = new java.util.LinkedHashMap<>();
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

    @Override
    public S3PresignedUrlResult generateUploadUrl(Long userId, String extension, String contentType) {
        String objectKey = S3PathConstants.certificationPrefix(userId) + UUID.randomUUID() + "." + extension;
        return s3PresignService.presignPut(objectKey, contentType);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findApprovedUserIdsByFestivalId(Long festivalId) {
        return certificationRepository.findApprovedUserIdsByFestivalId(festivalId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsApprovedCertification(Long festivalId, Long userId) {
        return certificationRepository.existsApprovedCertification(festivalId, userId);
    }
}
