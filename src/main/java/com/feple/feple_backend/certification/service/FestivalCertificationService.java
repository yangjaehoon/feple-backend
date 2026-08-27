package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.certification.dto.CertificationResponseDto;
import com.feple.feple_backend.file.dto.S3PresignedUrlResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface FestivalCertificationService {
    CertificationResponseDto submit(Long userId, Long festivalId, String photoKey);
    List<CertificationResponseDto> getMyCertifications(Long userId);
    List<Long> getApprovedFestivalIds(Long userId);
    long countApprovedByUser(Long userId);
    List<CertificationResponseDto> getPublicCertifications(Long userId);
    Map<String, Object> getCertDetail(Long userId, Long festivalId);
    S3PresignedUrlResult generateUploadUrl(Long userId, String extension, String contentType);
    Set<Long> findApprovedUserIdsByFestivalId(Long festivalId);

    /** {@link #findApprovedUserIdsByFestivalId(Long)}의 범위 한정 버전 — 후보 작성자 ID로만 조회한다 */
    Set<Long> findApprovedUserIdsByFestivalId(Long festivalId, Collection<Long> candidateUserIds);
    boolean existsApprovedCertification(Long festivalId, Long userId);
    void removeAllByUser(Long userId);
    void removeAllByFestival(Long festivalId);
}
