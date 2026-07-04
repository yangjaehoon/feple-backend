package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface FestivalCertificationAdminService {
    Page<FestivalCertification> getByStatus(CertificationStatus status, int page);
    Page<FestivalCertification> searchByKeyword(String keyword, CertificationStatus status, int page);
    FestivalCertification getById(Long id);
    void approve(Long certId, String reviewerName);
    void reject(Long certId, String rejectionMessage, String reviewerName);
    void bulkApprove(List<Long> ids, String reviewerName);
    void bulkReject(List<Long> ids, String rejectionMessage, String reviewerName);
    List<FestivalCertification> getByUserId(Long userId);
    long getPendingCount();
    Optional<Long> findNextPendingId(Long currentId);
    String buildPhotoUrl(String photoKey);
}
