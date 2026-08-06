package com.feple.feple_backend.admin.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.feple.feple_backend.comment.entity.CommentReport;
import com.feple.feple_backend.comment.service.CommentReportService;
import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.post.entity.ReportReason;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentReportCsvExporterTest {

    @Mock CommentReportService commentReportService;

    @InjectMocks CommentReportCsvExporter exporter;

    @Test
    void getReportType은_comment() {
        assertThat(exporter.getReportType()).isEqualTo("comment");
    }

    @Test
    void buildCsv_헤더와_신고없으면_헤더만_반환() {
        given(commentReportService.getAllCommentReportsForExport()).willReturn(List.of());

        String csv = exporter.buildCsv();

        assertThat(csv).isEqualTo("ID,신고일시,댓글ID,댓글내용,게시글제목,댓글작성자,신고자,사유,상세,상태\n");
    }

    @Test
    void buildCsv_신고건이_행으로_추가됨() {
        CommentReport report = mock(CommentReport.class);
        given(report.getId()).willReturn(1L);
        given(report.getCreatedAt()).willReturn(LocalDateTime.of(2026, 8, 1, 12, 0, 0));
        given(report.getCommentId()).willReturn(10L);
        given(report.getCommentContent()).willReturn("욕설 댓글");
        given(report.getCommentPostTitle()).willReturn("자유게시판 글");
        given(report.getCommentUserNickname()).willReturn("작성자닉");
        given(report.getReporterNickname()).willReturn("신고자닉");
        given(report.getReason()).willReturn(ReportReason.ABUSE);
        given(report.getDetail()).willReturn("상세 사유");
        given(report.getStatus()).willReturn(ReportStatus.PENDING);
        given(commentReportService.getAllCommentReportsForExport()).willReturn(List.of(report));

        String csv = exporter.buildCsv();

        assertThat(csv).contains("1,2026-08-01 12:00:00,10,욕설 댓글,자유게시판 글,작성자닉,신고자닉,ABUSE,상세 사유,PENDING");
    }
}
