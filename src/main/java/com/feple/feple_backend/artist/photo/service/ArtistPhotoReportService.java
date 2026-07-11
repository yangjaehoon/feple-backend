package com.feple.feple_backend.artist.photo.service;

import com.feple.feple_backend.admin.service.ReportAdminService;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhoto;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhotoReport;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoReportRepository;
import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.global.QueryResultMapper;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.ReportRejectionService;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.post.dto.ReportSubmitRequest;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.feple.feple_backend.global.cache.EvictAdminReportCaches;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistPhotoReportService implements ReportAdminService<ArtistGalleryPhotoReport> {

    private final ArtistGalleryPhotoReportRepository reportRepository;
    private final ArtistGalleryPhotoRepository photoRepository;
    private final ArtistGalleryPhotoLikeRepository photoLikeRepository;
    private final UserRepository userRepository;
    private final S3PresignService s3PresignService;

    // PostReportService/CommentReportService의 submitReport와 구조가 동일하지만,
    // 빌더 타입이 전부 달라 제네릭으로 묶으면 콜백만 많아지고 오히려 읽기 어려워져 통합하지 않는다.
    @Transactional
    public void submitReport(Long photoId, Long reporterId, ReportSubmitRequest command) {
        if (reportRepository.existsByReporterIdAndPhotoId(reporterId, photoId)) {
            throw new ConflictException("이미 신고한 사진입니다.");
        }
        ArtistGalleryPhoto photo = EntityLoader.getOrThrow(photoRepository::findById, photoId, "사진");
        User reporter = EntityLoader.getOrThrow(userRepository::findById, reporterId, "사용자");

        reportRepository.save(ArtistGalleryPhotoReport.builder()
                .photo(photo)
                .reporter(reporter)
                .reason(command.reason())
                .detail(command.detail())
                .build());
    }

    @Override
    @Cacheable(value = "adminReportTypeCounts", key = "'photoPending'")
    public long getPendingCount() {
        return reportRepository.countByStatus(ReportStatus.PENDING);
    }

    @Override
    @Cacheable(value = "adminReportTypeCounts", key = "'photoTotal'")
    public long getTotalCount() {
        return reportRepository.count();
    }

    @Override
    public String getReportType() { return "photo"; }

    @Override
    public Page<ArtistGalleryPhotoReport> findPendingReports(PageRequest pageable) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable);
    }

    @Override
    public Page<ArtistGalleryPhotoReport> findAllReports(PageRequest pageable) {
        return reportRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public Page<ArtistGalleryPhotoReport> searchReportsByKeyword(String keyword, ReportStatus status, PageRequest pageable) {
        return reportRepository.searchByKeyword(keyword, status, pageable);
    }

    // 사진은 소프트 삭제가 없어 하드 삭제하며, FK 제약상 신고 레코드도 함께
    // 하드 삭제됨(PostReportService처럼 레코드 보존+resolve가 아님) —
    // ReportCommandService.deleteContentAndResolve 계약 문서 참고
    @Override
    @EvictAdminReportCaches
    @Transactional
    public void deleteContentAndResolve(Long reportId) {
        deletePhotoAndResolve(reportId);
    }

    @Transactional
    public void deletePhotoAndResolve(Long reportId) {
        ArtistGalleryPhotoReport report = EntityLoader.getOrThrow(reportRepository::findById, reportId, "신고");
        Long photoId = report.getPhotoId();

        // FK 순서: ArtistGalleryPhotoReport → ArtistGalleryPhotoLike → ArtistGalleryPhoto
        reportRepository.deleteAllByPhotoId(photoId);
        photoLikeRepository.deleteByPhotoId(photoId);
        photoRepository.deleteById(photoId);
    }

    @EvictAdminReportCaches
    @Transactional
    public void dismissReport(Long reportId) {
        ReportRejectionService.reject(reportRepository, reportId);
    }

    @Override
    @EvictAdminReportCaches
    @Transactional
    public void bulkDismiss(List<Long> ids) {
        ReportRejectionService.bulkDismiss(reportRepository, ids);
    }

    @Override
    public Map<Long, String> buildPhotoPresignedUrls(Page<?> reports) {
        return reports.getContent().stream()
                .filter(r -> r instanceof ArtistGalleryPhotoReport)
                .map(r -> (ArtistGalleryPhotoReport) r)
                .collect(Collectors.toMap(ArtistGalleryPhotoReport::getPhotoId,
                        r -> s3PresignService.presignGetUrl(r.getPhotoKey())));
    }

    @Override
    public Long extractAuthorId(ArtistGalleryPhotoReport report) { return report.getPhotoUploaderId(); }

    @Override
    public Map<Long, Long> getAuthorReportCounts(Collection<Long> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return QueryResultMapper.toLongMap(reportRepository.countByPhotoUploaderIds(userIds));
    }

    public long getReportCountForUser(Long userId) {
        return QueryResultMapper.extractSingleCount(reportRepository.countByPhotoUploaderIds(List.of(userId)));
    }
}
