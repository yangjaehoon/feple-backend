package com.feple.feple_backend.admin.csv;

import com.feple.feple_backend.artist.photo.service.ArtistPhotoReportService;
import com.feple.feple_backend.global.ReportTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ArtistPhotoReportCsvExporter implements ReportCsvExporter {

    private final ArtistPhotoReportService artistPhotoReportService;

    @Override
    public String getReportType() { return ReportTypes.PHOTO; }

    @Override
    public String buildCsv() {
        return CsvExporter.buildCsv("ID,신고일시,사진ID,아티스트,업로더,신고자,사유,상세,상태\n",
                artistPhotoReportService.getAllPhotoReportsForExport(),
                r -> new Object[]{
                        r.getId(), CsvExporter.formatDt(r.getCreatedAt()), r.getPhotoId(), r.getPhotoArtistName(),
                        r.getPhotoUploaderNickname(), r.getReporterNickname(),
                        r.getReason().name(), r.getDetail(), r.getStatus().name() });
    }
}
