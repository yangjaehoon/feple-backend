package com.feple.feple_backend.admin;

import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.service.PostReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PostReportCsvExporter implements ReportCsvExporter {

    private final PostReportService postReportService;

    @Override
    public String getReportType() { return AdminConstants.REPORT_TYPE_POST; }

    @Override
    public String buildCsv() {
        StringBuilder sb = new StringBuilder("ID,신고일시,게시글ID,게시글제목,게시자,신고자,사유,상세,상태\n");
        for (PostReport r : postReportService.getAllPostReportsForExport()) {
            sb.append(CsvExporter.row(
                    r.getId(),
                    CsvExporter.formatDt(r.getCreatedAt()),
                    r.getPostId(),
                    r.getPostTitle(),
                    r.getPosterNickname(),
                    r.getReporterNickname(),
                    r.getReason().name(),
                    r.getDetail(),
                    r.getStatus().name()));
        }
        return sb.toString();
    }
}
