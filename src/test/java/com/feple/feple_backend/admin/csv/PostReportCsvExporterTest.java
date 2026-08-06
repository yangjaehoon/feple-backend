package com.feple.feple_backend.admin.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.feple.feple_backend.global.entity.ReportStatus;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.post.service.PostReportService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostReportCsvExporterTest {

    @Mock PostReportService postReportService;

    @InjectMocks PostReportCsvExporter exporter;

    @Test
    void getReportType은_post() {
        assertThat(exporter.getReportType()).isEqualTo("post");
    }

    @Test
    void buildCsv_신고없으면_헤더만_반환() {
        given(postReportService.getAllPostReportsForExport()).willReturn(List.of());

        String csv = exporter.buildCsv();

        assertThat(csv).isEqualTo("ID,신고일시,게시글ID,게시글제목,게시자,신고자,사유,상세,상태\n");
    }

    @Test
    void buildCsv_신고건이_행으로_추가됨() {
        PostReport report = mock(PostReport.class);
        given(report.getId()).willReturn(1L);
        given(report.getCreatedAt()).willReturn(LocalDateTime.of(2026, 8, 1, 12, 0, 0));
        given(report.getPostId()).willReturn(20L);
        given(report.getPostTitle()).willReturn("스팸 게시글");
        given(report.getAuthorNickname()).willReturn("작성자닉");
        given(report.getReporterNickname()).willReturn("신고자닉");
        given(report.getReason()).willReturn(ReportReason.SPAM);
        given(report.getDetail()).willReturn("상세 사유");
        given(report.getStatus()).willReturn(ReportStatus.PENDING);
        given(postReportService.getAllPostReportsForExport()).willReturn(List.of(report));

        String csv = exporter.buildCsv();

        assertThat(csv).contains("1,2026-08-01 12:00:00,20,스팸 게시글,작성자닉,신고자닉,SPAM,상세 사유,PENDING");
    }
}
