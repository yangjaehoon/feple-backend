package com.feple.feple_backend.admin.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

class CsvExporterTest {

    // ── cell ─────────────────────────────────────────────────────────────

    @Test
    void cell_null이면_빈문자열() {
        assertThat(CsvExporter.cell(null)).isEmpty();
    }

    @Test
    void cell_일반값은_그대로_문자열화() {
        assertThat(CsvExporter.cell(123)).isEqualTo("123");
        assertThat(CsvExporter.cell("일반텍스트")).isEqualTo("일반텍스트");
    }

    @Test
    void cell_수식으로_해석될수있는_접두문자는_탭으로_차단() {
        assertThat(CsvExporter.cell("=SUM(A1)")).isEqualTo("\t=SUM(A1)");
        assertThat(CsvExporter.cell("+1234")).isEqualTo("\t+1234");
        assertThat(CsvExporter.cell("-1234")).isEqualTo("\t-1234");
        assertThat(CsvExporter.cell("@mention")).isEqualTo("\t@mention");
    }

    @Test
    void cell_콤마_포함시_따옴표로_감싼다() {
        assertThat(CsvExporter.cell("a,b")).isEqualTo("\"a,b\"");
    }

    @Test
    void cell_따옴표_포함시_이스케이프하고_감싼다() {
        assertThat(CsvExporter.cell("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
    }

    @Test
    void cell_개행_포함시_따옴표로_감싼다() {
        assertThat(CsvExporter.cell("line1\nline2")).isEqualTo("\"line1\nline2\"");
    }

    // ── formatDt ─────────────────────────────────────────────────────────

    @Test
    void formatDt_null이면_빈문자열() {
        assertThat(CsvExporter.formatDt(null)).isEmpty();
    }

    @Test
    void formatDt_정상_포맷() {
        LocalDateTime dt = LocalDateTime.of(2026, 8, 1, 13, 5, 30);

        assertThat(CsvExporter.formatDt(dt)).isEqualTo("2026-08-01 13:05:30");
    }

    // ── row ──────────────────────────────────────────────────────────────

    @Test
    void row_값들을_콤마로_연결하고_개행으로_끝난다() {
        assertThat(CsvExporter.row(1, "제목", null)).isEqualTo("1,제목,\n");
    }

    // ── csvResponse ──────────────────────────────────────────────────────

    @Test
    void csvResponse_UTF8_BOM_포함() {
        ResponseEntity<byte[]> response = CsvExporter.csvResponse("id,name\n1,test\n", "report.csv");

        byte[] body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body[0]).isEqualTo((byte) 0xEF);
        assertThat(body[1]).isEqualTo((byte) 0xBB);
        assertThat(body[2]).isEqualTo((byte) 0xBF);
    }

    @Test
    void csvResponse_헤더_설정() {
        ResponseEntity<byte[]> response = CsvExporter.csvResponse("content", "신고내역.csv");

        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("text/csv;charset=UTF-8");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("attachment; filename*=UTF-8''");
    }
}
