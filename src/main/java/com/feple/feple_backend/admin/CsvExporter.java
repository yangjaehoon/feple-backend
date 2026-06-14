package com.feple.feple_backend.admin;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class CsvExporter {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CsvExporter() {}

    public static String cell(Object value) {
        if (value == null) return "";
        String s = value.toString();
        // =, +, @, - 로 시작하는 값은 스프레드시트 수식으로 해석될 수 있으므로 탭 문자를 앞에 붙여 차단
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) {
            s = "\t" + s;
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r") || s.contains("\t")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public static String formatDt(LocalDateTime dt) {
        return dt == null ? "" : dt.format(DT_FMT);
    }

    public static String row(Object... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(cell(values[i]));
        }
        return sb.append('\n').toString();
    }

    public static ResponseEntity<byte[]> csvResponse(String content, String filename) {
        byte[] bom    = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] body   = content.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + body.length];
        System.arraycopy(bom,  0, result, 0,          bom.length);
        System.arraycopy(body, 0, result, bom.length, body.length);
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(result);
    }
}
