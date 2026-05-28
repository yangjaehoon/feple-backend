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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

        // FK 순서: ArtistPhotoReport → ArtistGalleryPhotoLike → ArtistGalleryPhoto
        reportRepository.deleteAll(reportRepository.findByPhotoId(photoId));
        photoLikeRepository.deleteByPhotoId(photoId);
        photoRepository.deleteById(photoId);
    }

    @Transactional
    public void dismissReport(Long reportId) {
        ArtistPhotoReport report = EntityFinder.getOrThrow(reportRepository::findById, reportId, "신고");
        report.resolve(ReportStatus.DISMISSED);
    }

    @Override
    @Transactional
    public void bulkDismiss(List<Long> ids) {
        if (ids.isEmpty()) return;
        reportRepository.findAllById(ids).stream()
                .filter(ArtistPhotoReport::isPending)
                .forEach(r -> r.resolve(ReportStatus.DISMISSED));
    }

    public Map<Long, Long> getUploaderReportCounts(Collection<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return reportRepository.countByPhotoUploaderIds(userIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    public long getReportCountForUser(Long userId) {
        List<Object[]> result = reportRepository.countByPhotoUploaderIds(List.of(userId));
        return result.isEmpty() ? 0L : (Long) result.get(0)[1];
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, Long> buildAuthorReportCounts(Page<?> reports) {
        Set<Long> ids = ((Page<ArtistPhotoReport>) reports).getContent().stream()
                .map(ArtistPhotoReport::getPhotoUploaderId).collect(Collectors.toSet());
        return getUploaderReportCounts(ids);
    }
}
