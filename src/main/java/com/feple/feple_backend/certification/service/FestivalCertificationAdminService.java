package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface FestivalCertificationAdminService {
    Page<FestivalCertification> getByStatus(CertificationStatus status, int page);
    Page<FestivalCertification> searchByKeyword(String keyword, CertificationStatus status, int page);
    FestivalCertification getById(Long id);
    void approve(Long certId, String reviewerName);
    void reject(Long certId, String rejectionMessage, String reviewerName);
    long getPendingCount();
    Optional<Long> findNextPendingId(Long currentId);
    String buildPhotoUrl(String photoKey);
}
