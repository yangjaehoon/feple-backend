package com.feple.feple_backend.admin.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhotoReport;
import com.feple.feple_backend.artist.photo.service.ArtistPhotoReportService;
import com.feple.feple_backend.global.entity.ReportReason;
import com.feple.feple_backend.global.entity.ReportStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtistPhotoReportCsvExporterTest {

    @Mock ArtistPhotoReportService artistPhotoReportService;

    @InjectMocks ArtistPhotoReportCsvExporter exporter;

    @Test
    void getReportType은_photo() {
        assertThat(exporter.getReportType()).isEqualTo("photo");
    }

    @Test
    void buildCsv_신고없으면_헤더만_반환() {
        given(artistPhotoReportService.getAllPhotoReportsForExport()).willReturn(List.of());

        String csv = exporter.buildCsv();

        assertThat(csv).isEqualTo("ID,신고일시,사진ID,아티스트,업로더,신고자,사유,상세,상태\n");
    }

    @Test
    void buildCsv_신고건이_행으로_추가됨() {
        ArtistGalleryPhotoReport report = mock(ArtistGalleryPhotoReport.class);
        given(report.getId()).willReturn(1L);
        given(report.getCreatedAt()).willReturn(LocalDateTime.of(2026, 8, 1, 12, 0, 0));
        given(report.getPhotoId()).willReturn(30L);
        given(report.getPhotoArtistName()).willReturn("아이유");
        given(report.getPhotoUploaderNickname()).willReturn("업로더닉");
        given(report.getReporterNickname()).willReturn("신고자닉");
        given(report.getReason()).willReturn(ReportReason.SPAM);
        given(report.getDetail()).willReturn("상세 사유");
        given(report.getStatus()).willReturn(ReportStatus.PENDING);
        given(artistPhotoReportService.getAllPhotoReportsForExport()).willReturn(List.of(report));

        String csv = exporter.buildCsv();

        assertThat(csv).contains("1,2026-08-01 12:00:00,30,아이유,업로더닉,신고자닉,SPAM,상세 사유,PENDING");
    }
}
