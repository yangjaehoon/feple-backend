package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.certification.dto.CertificationResponseDto;
import com.feple.feple_backend.file.dto.PresignResult;

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
    PresignResult generateUploadUrl(Long userId, String extension, String contentType);
    Set<Long> findApprovedUserIdsByFestivalId(Long festivalId);
    boolean existsApprovedCertification(Long festivalId, Long userId);
}
