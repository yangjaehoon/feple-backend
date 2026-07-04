package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.LikeEscaper;
import com.feple.feple_backend.global.PageableFactory;
import com.feple.feple_backend.global.cache.EvictAdminPendingCaches;
import com.feple.feple_backend.notification.service.NotificationService;
import com.feple.feple_backend.user.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FestivalCertificationAdminServiceImpl implements FestivalCertificationAdminService {

    private final FestivalCertificationRepository certificationRepository;
    private final S3PresignService s3PresignService;
    private final NotificationService notificationService;
    private final PointService pointService;

    @Override
    @Transactional(readOnly = true)
    public Page<FestivalCertification> getByStatus(CertificationStatus status, int page) {
        Pageable pageable = PageableFactory.latestFirst(page, 20);
        if (status == null) {
            return certificationRepository.findAll(pageable);
        }
        return certificationRepository.findByStatus(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FestivalCertification> searchByKeyword(String keyword, CertificationStatus status, int page) {
        Pageable pageable = PageableFactory.latestFirst(page, 20);
        return certificationRepository.searchByKeyword(LikeEscaper.escape(keyword.trim()), status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public FestivalCertification getById(Long id) {
        return EntityFinder.getOrThrow(certificationRepository::findWithUserAndFestivalById, id, "인증 신청");
    }

    @Override
    @EvictAdminPendingCaches
    @Transactional
    public void approve(Long certId, String reviewerName) {
        FestivalCertification cert = getById(certId);
        cert.approve(reviewerName);
        pointService.addCertApprovedPoint(cert.getUserId(), certId);
        // 비동기 알림
        notificationService.notifyCertApproved(
                cert.getUserId(),
                cert.getFestivalTitle(),
                cert.getFestivalTitleEn(),
                cert.getFestivalId());
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public long getPendingCount() {
        return certificationRepository.countByStatus(CertificationStatus.PENDING);
    }

    @Override
    public String buildPhotoUrl(String photoKey) {
        return s3PresignService.presignGetUrl(photoKey);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findNextPendingId(Long currentId) {
        List<Long> ids = certificationRepository.findNextPendingIds(currentId, PageRequest.of(0, 1));
        return ids.isEmpty() ? Optional.empty() : Optional.of(ids.get(0));
    }
}
