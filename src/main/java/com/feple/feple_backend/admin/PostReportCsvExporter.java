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
    public String getReportType() { return "post"; }

    @Override
    public String buildCsv() {
        StringBuilder sb = new StringBuilder("ID,신고일시,게시글ID,게시글제목,게시자,신고자,사유,상세,상태\n");
        for (PostReport r : postReportService.getAllPostReportsForExport()) {
            sb.append(CsvExporter.cell(r.getId()))
              .append(',').append(CsvExporter.cell(CsvExporter.formatDt(r.getCreatedAt())))
              .append(',').append(CsvExporter.cell(r.getPostId()))
              .append(',').append(CsvExporter.cell(r.getPostTitle()))
              .append(',').append(CsvExporter.cell(r.getPosterNickname()))
              .append(',').append(CsvExporter.cell(r.getReporterNickname()))
              .append(',').append(CsvExporter.cell(r.getReason().name()))
              .append(',').append(CsvExporter.cell(r.getDetail()))
              .append(',').append(CsvExporter.cell(r.getStatus().name()))
              .append('\n');
        }
        return sb.toString();
    }
}
