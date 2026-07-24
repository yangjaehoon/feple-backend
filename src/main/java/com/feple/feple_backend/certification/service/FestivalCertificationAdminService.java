package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FestivalCertificationAdminService {
    /** 해당 페스티벌에 인증(APPROVED)된 유저 ID 목록 */
    Set<Long> getApprovedUserIds(Long festivalId);
    Page<FestivalCertification> getByStatus(CertificationStatus status, int page);
    Page<FestivalCertification> searchByKeyword(String keyword, CertificationStatus status, int page);
    FestivalCertification getById(Long id);
    void approve(Long certId, String reviewerName);
    void reject(Long certId, String rejectionMessage, String reviewerName);
    void bulkApprove(List<Long> ids, String reviewerName);
    void bulkReject(List<Long> ids, String rejectionMessage, String reviewerName);
    List<FestivalCertification> getByUserId(Long userId);
    /** 대시보드 미리보기용 — 대기중 인증을 최신순 최대 limit건 */
    List<FestivalCertification> getPendingPreview(int limit);
    long getPendingCount();
    Optional<Long> findNextPendingId(Long currentId);
    String buildPhotoUrl(String photoKey);
}
