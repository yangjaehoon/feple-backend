package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.event.CertificationApprovedEvent;
import com.feple.feple_backend.certification.event.CertificationRejectedEvent;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.global.EntityRequirer;
import com.feple.feple_backend.global.JpqlLikeEscaper;
import com.feple.feple_backend.global.PageableFactory;
import com.feple.feple_backend.global.cache.EvictAdminPendingCaches;
import com.feple.feple_backend.user.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
    private final PointService pointService;

    @Override
    @Transactional(readOnly = true)
    public Page<FestivalCertification> getByStatus(CertificationStatus status, int page) {
        Pageable pageable = PageableFactory.latestFirst(page, AdminConstants.LIST_PAGE_SIZE);
        if (status == null) {
            return certificationRepository.findAll(pageable);
        }
        return certificationRepository.findByStatus(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FestivalCertification> searchByKeyword(String keyword, CertificationStatus status, int page) {
        Pageable pageable = PageableFactory.latestFirst(page, AdminConstants.LIST_PAGE_SIZE);
        return certificationRepository.searchByKeyword(JpqlLikeEscaper.escape(keyword.trim()), status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public FestivalCertification getById(Long id) {
        return EntityRequirer.getOrThrow(certificationRepository::findWithUserAndFestivalById, id, "인증 신청");
    }

    @Override
    @EvictAdminPendingCaches
    @Transactional
    public void approve(Long certId, String reviewerName) {
        FestivalCertification cert = getById(certId);
        cert.approve(reviewerName);
        pointService.addCertApprovedPoint(cert.getUserId(), certId);
        // 트랜잭션 커밋 후에만 알림 발송 (커밋 실패 시 승인되지 않은 알림이 나가는 것을 방지)
        eventPublisher.publishEvent(new CertificationApprovedEvent(
                cert.getUserId(),
                cert.getFestivalTitle(),
                cert.getFestivalTitleEn(),
                cert.getFestivalId()));
    }

    @Override
    @EvictAdminPendingCaches
    @Transactional
    public void reject(Long certId, String rejectionMessage, String reviewerName) {
        FestivalCertification cert = getById(certId);
        cert.reject(rejectionMessage, reviewerName);
        // 트랜잭션 커밋 후에만 알림 발송 (커밋 실패 시 거절되지 않은 알림이 나가는 것을 방지)
        eventPublisher.publishEvent(new CertificationRejectedEvent(
                cert.getUserId(),
                cert.getFestivalTitle(),
                cert.getFestivalTitleEn(),
                cert.getFestivalId(),
                rejectionMessage));
    }

    @Override
    @EvictAdminPendingCaches
    @Transactional
    public void bulkApprove(List<Long> ids, String reviewerName) {
        certificationRepository.findWithUserAndFestivalByIdIn(ids).stream()
                .filter(FestivalCertification::isPending)
                .forEach(cert -> {
                    cert.approve(reviewerName);
                    pointService.addCertApprovedPoint(cert.getUserId(), cert.getId());
                    eventPublisher.publishEvent(new CertificationApprovedEvent(
                            cert.getUserId(),
                            cert.getFestivalTitle(),
                            cert.getFestivalTitleEn(),
                            cert.getFestivalId()));
                });
    }

    @Override
    @EvictAdminPendingCaches
    @Transactional
    public void bulkReject(List<Long> ids, String rejectionMessage, String reviewerName) {
        certificationRepository.findWithUserAndFestivalByIdIn(ids).stream()
                .filter(FestivalCertification::isPending)
                .forEach(cert -> {
                    cert.reject(rejectionMessage, reviewerName);
                    eventPublisher.publishEvent(new CertificationRejectedEvent(
                            cert.getUserId(),
                            cert.getFestivalTitle(),
                            cert.getFestivalTitleEn(),
                            cert.getFestivalId(),
                            rejectionMessage));
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<FestivalCertification> getByUserId(Long userId) {
        return certificationRepository.findByUserId(userId);
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
