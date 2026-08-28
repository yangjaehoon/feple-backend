package com.feple.feple_backend.admin.csv;

import com.feple.feple_backend.admin.support.AdminConstants;
import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhotoReport;
import com.feple.feple_backend.artist.photo.service.ArtistPhotoReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ArtistPhotoReportCsvExporter implements ReportCsvExporter {

    private final ArtistPhotoReportService artistPhotoReportService;

    @Override
    public String getReportType() { return AdminConstants.REPORT_TYPE_PHOTO; }

    @Override
    public String buildCsv() {
        StringBuilder sb = new StringBuilder("ID,신고일시,사진ID,아티스트,업로더,신고자,사유,상세,상태\n");
        for (ArtistGalleryPhotoReport r : artistPhotoReportService.getAllPhotoReportsForExport()) {
            sb.append(CsvExporter.row(
                    r.getId(),
                    CsvExporter.formatDt(r.getCreatedAt()),
                    r.getPhotoId(),
                    r.getPhotoArtistName(),
                    r.getPhotoUploaderNickname(),
                    r.getReporterNickname(),
                    r.getReason().name(),
                    r.getDetail(),
                    r.getStatus().name()));
        }
        return sb.toString();
    }
}
