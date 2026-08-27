package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.certification.dto.CertificationResponseDto;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.repository.CertificationReviewLikeRepository;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.S3PathConstants;
import com.feple.feple_backend.file.dto.S3PresignedUrlResult;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.file.service.S3ObjectVerificationService;
import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class FestivalCertificationServiceImpl implements FestivalCertificationService {

    private final FestivalCertificationRepository certificationRepository;
    private final UserRepository userRepository;
    private final FestivalRepository festivalRepository;
    private final S3PresignService s3PresignService;
    private final S3ObjectVerificationService s3ObjectVerificationService;
    private final FileStorageService fileStorageService;
    private final CertificationReviewLikeRepository reviewLikeRepository;
    private final TransactionTemplate transactionTemplate;

    // S3 검증(headObject)은 커넥션 점유 없이 수행; 이후 각 리포지토리 호출이
    // 자체 트랜잭션으로 DB에 반영한다 (ArtistGalleryPhotoService.register와 동일 패턴)
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public CertificationResponseDto submit(Long userId, Long festivalId, String photoKey) {
        validateUpload(userId, photoKey);

        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        Festival festival = EntityLoader.getOrThrow(festivalRepository::findById, festivalId, "페스티벌");
        FestivalCertification cert = saveCertification(user, festival, userId, photoKey);
        return toDto(cert);
    }

    // presign만 받고 실제 업로드하지 않은 채로 제출하면 영구히 깨진 이미지 레코드가 생성되므로
    // ArtistGalleryPhotoService.register()와 동일하게 S3 오브젝트 존재 여부를 검증한다
    private void validateUpload(Long userId, String photoKey) {
        S3PathConstants.requireWithinPrefix(photoKey, S3PathConstants.certificationPrefix(userId));
        s3ObjectVerificationService.verifyImageObject(photoKey);
    }

    // 거절된 인증은 재신청이 가능해야 한다 — (user_id, festival_id) 유니크 제약 때문에 기존
    // REJECTED 레코드를 지우지 않으면 재제출이 항상 409로 막힌다. PENDING/APPROVED는 계속 차단.
    // 기존 레코드 삭제와 새 레코드 저장은 하나의 트랜잭션으로 묶어야 한다 — 따로 커밋되면 삭제만
    // 성공하고 저장이 실패했을 때 거절 이력이 조용히 사라질 수 있다. submit()이 NOT_SUPPORTED라
    // self-invocation으로는 @Transactional이 걸리지 않으므로 TransactionTemplate을 직접 사용한다.
    private FestivalCertification saveCertification(User user, Festival festival, Long userId, String photoKey) {
        FestivalCertification existing =
                certificationRepository.findByUserIdAndFestivalId(userId, festival.getId()).orElse(null);
        if (existing != null && existing.getStatus() != CertificationStatus.REJECTED) {
            throw new ConflictException("이미 해당 페스티벌에 인증 신청을 했습니다.");
        }

        FestivalCertification cert = FestivalCertification.create(user, festival, photoKey);
        try {
            transactionTemplate.executeWithoutResult(status -> {
                if (existing != null) {
                    certificationRepository.delete(existing);
                    fileStorageService.deleteFileAfterCommit(existing.getPhotoKey());
                }
                certificationRepository.saveAndFlush(cert);
            });
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("이미 해당 페스티벌에 인증 신청을 했습니다.");
        }
        return cert;
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
    public Set<Long> findApprovedUserIdsByFestivalId(Long festivalId, Collection<Long> candidateUserIds) {
        if (candidateUserIds.isEmpty()) {
            return Set.of();
        }
        return certificationRepository.findApprovedUserIdsByFestivalIdAndUserIdIn(festivalId, candidateUserIds);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsApprovedCertification(Long festivalId, Long userId) {
        return certificationRepository.existsApprovedCertification(festivalId, userId);
    }

    @Override
    @Transactional
    public void removeAllByUser(Long userId) {
        // 벌크 DELETE 쿼리라 삭제될 row의 photoKey를 미리 읽어둬야 S3 정리가 가능하다
        certificationRepository.findByUserId(userId)
                .forEach(cert -> fileStorageService.deleteFileAfterCommit(cert.getPhotoKey()));
        certificationRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void removeAllByFestival(Long festivalId) {
        certificationRepository.findByFestivalId(festivalId)
                .forEach(cert -> fileStorageService.deleteFileAfterCommit(cert.getPhotoKey()));
        reviewLikeRepository.deleteByCertificationFestivalId(festivalId);
        certificationRepository.deleteByFestivalId(festivalId);
    }
}
