package com.feple.feple_backend.artist.photo.service;

import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhoto;
import com.feple.feple_backend.artist.photo.entity.ArtistPhotoReport;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistPhotoReportRepository;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ArtistPhotoReportService {

    private final ArtistPhotoReportRepository reportRepository;
    private final ArtistGalleryPhotoRepository photoRepository;
    private final UserRepository userRepository;

    @Transactional
    public void submitReport(Long photoId, Long reporterId, ReportReason reason, String detail) {
        if (reportRepository.existsByReporterIdAndPhotoId(reporterId, photoId)) {
            throw new IllegalStateException("이미 신고한 사진입니다.");
        }
        ArtistGalleryPhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new NoSuchElementException("사진을 찾을 수 없습니다: " + photoId));
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + reporterId));

        reportRepository.save(ArtistPhotoReport.builder()
                .photo(photo)
                .reporter(reporter)
                .reason(reason)
                .detail(detail)
                .build());
    }
}
