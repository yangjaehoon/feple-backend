package com.feple.feple_backend.artist.photo.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhoto;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhotoReport;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistGalleryPhotoReportRepository;
import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.post.dto.ReportSubmitRequest;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.post.entity.ReportStatus;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArtistPhotoReportServiceTest {

    @Mock ArtistGalleryPhotoReportRepository reportRepository;
    @Mock ArtistGalleryPhotoRepository photoRepository;
    @Mock ArtistGalleryPhotoLikeRepository photoLikeRepository;
    @Mock UserRepository userRepository;
    @Mock S3PresignService s3PresignService;

    @InjectMocks ArtistPhotoReportService service;

    private Artist artist(Long id) {
        return Artist.builder().id(id).name("아티스트" + id).build();
    }

    private User user(Long id) {
        return User.builder().id(id).nickname("user" + id).build();
    }

    private ArtistGalleryPhoto photo(Long id, Artist artist, User uploader) {
        ArtistGalleryPhoto photo = new ArtistGalleryPhoto(artist, uploader,
                "artist-photos/" + artist.getId() + "/key.jpg", "image/jpeg", "title", "desc", false);
        ReflectionTestUtils.setField(photo, "id", id);
        return photo;
    }

    private ArtistGalleryPhotoReport report(Long id, ArtistGalleryPhoto photo, User reporter) {
        ArtistGalleryPhotoReport report = ArtistGalleryPhotoReport.builder()
                .photo(photo)
                .reporter(reporter)
                .reason(ReportReason.SPAM)
                .detail("상세")
                .build();
        ReflectionTestUtils.setField(report, "id", id);
        return report;
    }

    // ── submitReport ─────────────────────────────────────────────────────

    @Test
    void 신고_정상_제출() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        User reporter = user(20L);
        ArtistGalleryPhoto p = photo(5L, artist, uploader);
        given(reportRepository.existsByReporterIdAndPhotoId(20L, 5L)).willReturn(false);
        given(photoRepository.findById(5L)).willReturn(Optional.of(p));
        given(userRepository.findById(20L)).willReturn(Optional.of(reporter));

        service.submitReport(5L, 20L, new ReportSubmitRequest(ReportReason.SPAM, "상세"));

        verify(reportRepository).save(any(ArtistGalleryPhotoReport.class));
    }

    @Test
    void 신고_이미_신고한_사진이면_예외() {
        given(reportRepository.existsByReporterIdAndPhotoId(20L, 5L)).willReturn(true);

        assertThatThrownBy(() -> service.submitReport(5L, 20L, new ReportSubmitRequest(ReportReason.SPAM, "상세")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("이미 신고한");
        verify(reportRepository, never()).save(any());
    }

    // ── 카운트/조회 위임 ─────────────────────────────────────────────────

    @Test
    void getPendingCount는_PENDING_상태_카운트_위임() {
        given(reportRepository.countByStatus(ReportStatus.PENDING)).willReturn(3L);

        assertThat(service.getPendingCount()).isEqualTo(3L);
    }

    @Test
    void getTotalCount는_전체_카운트_위임() {
        given(reportRepository.count()).willReturn(7L);

        assertThat(service.getTotalCount()).isEqualTo(7L);
    }

    @Test
    void getReportType은_photo_반환() {
        assertThat(service.getReportType()).isEqualTo("photo");
    }

    @Test
    void findPendingReports는_리포지토리에_위임() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<ArtistGalleryPhotoReport> page = new PageImpl<>(List.of());
        given(reportRepository.findByStatusOrderByCreatedAtDesc(ReportStatus.PENDING, pageable)).willReturn(page);

        assertThat(service.findPendingReports(pageable)).isSameAs(page);
    }

    @Test
    void findAllReports는_리포지토리에_위임() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<ArtistGalleryPhotoReport> page = new PageImpl<>(List.of());
        given(reportRepository.findAllByOrderByCreatedAtDesc(pageable)).willReturn(page);

        assertThat(service.findAllReports(pageable)).isSameAs(page);
    }

    @Test
    void searchReportsByKeyword는_리포지토리에_위임() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<ArtistGalleryPhotoReport> page = new PageImpl<>(List.of());
        given(reportRepository.searchByKeyword("키워드", ReportStatus.PENDING, pageable)).willReturn(page);

        assertThat(service.searchReportsByKeyword("키워드", ReportStatus.PENDING, pageable)).isSameAs(page);
    }

    // ── deleteContentAndResolve / deletePhotoAndResolve ─────────────────

    @Test
    void 신고_승인시_사진_신고_좋아요_순서로_삭제() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        ArtistGalleryPhoto p = photo(5L, artist, uploader);
        ArtistGalleryPhotoReport r = report(1L, p, user(20L));
        given(reportRepository.findById(1L)).willReturn(Optional.of(r));

        service.deleteContentAndResolve(1L);

        verify(reportRepository).deleteAllByPhotoId(5L);
        verify(photoLikeRepository).deleteByPhotoId(5L);
        verify(photoRepository).deleteById(5L);
    }

    @Test
    void 존재하지_않는_신고_승인시_예외() {
        given(reportRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletePhotoAndResolve(1L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── dismissReport / bulkDismiss ──────────────────────────────────────

    @Test
    void 신고_기각시_상태가_REJECTED로_변경() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        ArtistGalleryPhoto p = photo(5L, artist, uploader);
        ArtistGalleryPhotoReport r = report(1L, p, user(20L));
        given(reportRepository.findById(1L)).willReturn(Optional.of(r));

        service.dismissReport(1L);

        assertThat(r.isPending()).isFalse();
    }

    @Test
    void 일괄_기각시_대기중인_신고만_REJECTED로_변경() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        ArtistGalleryPhoto p = photo(5L, artist, uploader);
        ArtistGalleryPhotoReport pending = report(1L, p, user(20L));
        ArtistGalleryPhotoReport resolved = report(2L, p, user(21L));
        resolved.resolve(ReportStatus.REJECTED);
        given(reportRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(pending, resolved));

        service.bulkDismiss(List.of(1L, 2L));

        assertThat(pending.isPending()).isFalse();
    }

    @Test
    void 일괄_기각_ID목록이_비어있으면_조회_생략() {
        service.bulkDismiss(List.of());

        verify(reportRepository, never()).findAllById(any());
    }

    // ── 부가 메서드 ──────────────────────────────────────────────────────

    @Test
    void buildPhotoPresignedUrls는_photoId별_presign_URL_맵() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        ArtistGalleryPhoto p = photo(5L, artist, uploader);
        ArtistGalleryPhotoReport r = report(1L, p, user(20L));
        Page<ArtistGalleryPhotoReport> page = new PageImpl<>(List.of(r));
        given(s3PresignService.presignGetUrl(p.getS3Key())).willReturn("https://url");

        Map<Long, String> result = service.buildPhotoPresignedUrls(page);

        assertThat(result).containsEntry(5L, "https://url");
    }

    @Test
    void extractAuthorId는_사진_업로더_ID_반환() {
        Artist artist = artist(1L);
        User uploader = user(10L);
        ArtistGalleryPhoto p = photo(5L, artist, uploader);
        ArtistGalleryPhotoReport r = report(1L, p, user(20L));

        assertThat(service.extractAuthorId(r)).isEqualTo(10L);
    }

    @Test
    void getAuthorReportCounts는_유저ID가_비어있으면_빈맵() {
        assertThat(service.getAuthorReportCounts(List.of())).isEmpty();
        verify(reportRepository, never()).countByPhotoUploaderIds(any());
    }

    @Test
    void getAuthorReportCounts는_유저별_신고건수_맵_반환() {
        given(reportRepository.countByPhotoUploaderIds(List.of(10L)))
                .willReturn(List.<Object[]>of(new Object[]{10L, 3L}));

        Map<Long, Long> result = service.getAuthorReportCounts(List.of(10L));

        assertThat(result).containsEntry(10L, 3L);
    }

    @Test
    void getReportCountForUser는_단건_카운트_반환() {
        given(reportRepository.countByPhotoUploaderIds(List.of(10L)))
                .willReturn(List.<Object[]>of(new Object[]{10L, 5L}));

        assertThat(service.getReportCountForUser(10L)).isEqualTo(5L);
    }
}
