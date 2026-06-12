package com.feple.feple_backend.admin;

import com.feple.feple_backend.comment.entity.CommentReport;
import com.feple.feple_backend.comment.service.CommentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class CommentReportCsvExporter implements ReportCsvExporter {

    private final CommentReportService commentReportService;

    @Override
    public String getReportType() { return AdminConstants.REPORT_TYPE_COMMENT; }

    @Override
    public String buildCsv() {
        StringBuilder sb = new StringBuilder("ID,신고일시,댓글ID,댓글내용,게시글제목,댓글작성자,신고자,사유,상세,상태\n");
        for (CommentReport r : commentReportService.getAllCommentReportsForExport()) {
            sb.append(CsvExporter.row(
                    r.getId(),
                    CsvExporter.formatDt(r.getCreatedAt()),
                    r.getCommentId(),
                    r.getCommentContent(),
                    r.getCommentPostTitle(),
                    r.getCommentUserNickname(),
                    r.getReporterNickname(),
                    r.getReason().name(),
                    r.getDetail(),
                    r.getStatus().name()));
        }
        return sb.toString();
    }
}
