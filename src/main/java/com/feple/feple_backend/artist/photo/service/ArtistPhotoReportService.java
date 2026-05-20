package com.feple.feple_backend.artist.photo.service;

import com.feple.feple_backend.admin.service.ReportAdminService;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhoto;
import com.feple.feple_backend.artist.photo.entity.ArtistPhotoReport;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistPhotoReportRepository;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistPhotoReportService implements ReportAdminService {

    private final ArtistPhotoReportRepository reportRepository;
    private final ArtistGalleryPhotoRepository photoRepository;
    private final ArtistGalleryPhotoLikeRepository photoLikeRepository;
    private final UserRepository userRepository;

    @Transactional
    public void submitReport(Long photoId, Long reporterId, ReportReason reason, String detail) {
        if (reportRepository.existsByReporterIdAndPhotoId(reporterId, photoId)) {
            throw new ConflictException("이미 신고한 사진입니다.");
        }
        ArtistGalleryPhoto photo = EntityFinder.getOrThrow(photoRepository::findById, photoId, "사진");
        User reporter = EntityFinder.getOrThrow(userRepository::findById, reporterId, "사용자");

        reportRepository.save(ArtistPhotoReport.builder()
                .photo(photo)
                .reporter(reporter)
                .reason(reason)
                .detail(detail)
                .build());
    }

    @Override
    public long getPendingCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    @Override
    public long getTotalCount() {
        return reportRepository.count();
    }

    @Override
    public Page<ArtistPhotoReport> getReportsForAdmin(int page, int size, String statusFilter) {
        PageRequest pageable = PageRequest.of(page, size);
        if ("PENDING".equals(statusFilter)) {
            return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable);
        }
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional
    public void deletePhotoAndResolve(Long reportId) {
        ArtistPhotoReport report = EntityFinder.getOrThrow(reportRepository::findById, reportId, "신고");
        Long photoId = report.getPhotoId();

        reportRepository.findByPhotoId(photoId).forEach(r -> r.resolve(ReportStatus.POST_DELETED));
        photoLikeRepository.deleteByPhotoId(photoId);
        photoRepository.deleteById(photoId);
    }

    @Transactional
    public void dismissReport(Long reportId) {
        ArtistPhotoReport report = EntityFinder.getOrThrow(reportRepository::findById, reportId, "신고");
        report.resolve(ReportStatus.DISMISSED);
    }
}
