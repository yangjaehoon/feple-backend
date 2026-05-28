package com.feple.feple_backend.admin;

import com.feple.feple_backend.comment.entity.CommentReport;
import com.feple.feple_backend.comment.service.CommentReportService;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.service.PostReportService;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/export")
@RequiredArgsConstructor
public class AdminCsvController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final UserAdminService userAdminService;
    private final PostReportService postReportService;
    private final CommentReportService commentReportService;

    @GetMapping("/users.csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportUsers() {
        List<UserResponseDto> users = userAdminService.getAllUsersForExport();
        StringBuilder sb = new StringBuilder();
        sb.append("ID,닉네임,이메일,역할,가입일,정지여부\n");
        for (UserResponseDto u : users) {
            sb.append(csv(u.getId()))
              .append(',').append(csv(u.getNickname()))
              .append(',').append(csv(u.getEmail()))
              .append(',').append(csv(u.getRole().getDisplayName()))
              .append(',').append(csv(formatDt(u.getCreatedAt())))
              .append(',').append(csv(u.isBanned() ? (u.isPermanentBan() ? "영구정지" : "정지중") : ""))
              .append('\n');
        }
        return csvResponse(sb.toString(), "users_" + LocalDate.now() + ".csv");
    }

    @GetMapping("/reports.csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportReports(@RequestParam(defaultValue = "post") String type) {
        StringBuilder sb = new StringBuilder();
        if ("comment".equals(type)) {
            sb.append("ID,신고일시,댓글ID,댓글내용,게시글제목,댓글작성자,신고자,사유,상세,상태\n");
            for (CommentReport r : commentReportService.getAllCommentReportsForExport()) {
                sb.append(csv(r.getId()))
                  .append(',').append(csv(formatDt(r.getCreatedAt())))
                  .append(',').append(csv(r.getCommentId()))
                  .append(',').append(csv(r.getCommentContent()))
                  .append(',').append(csv(r.getCommentPostTitle()))
                  .append(',').append(csv(r.getCommentUserNickname()))
                  .append(',').append(csv(r.getReporterNickname()))
                  .append(',').append(csv(r.getReason().name()))
                  .append(',').append(csv(r.getDetail()))
                  .append(',').append(csv(r.getStatus().name()))
                  .append('\n');
            }
        } else {
            sb.append("ID,신고일시,게시글ID,게시글제목,게시자,신고자,사유,상세,상태\n");
            for (PostReport r : postReportService.getAllPostReportsForExport()) {
                sb.append(csv(r.getId()))
                  .append(',').append(csv(formatDt(r.getCreatedAt())))
                  .append(',').append(csv(r.getPostId()))
                  .append(',').append(csv(r.getPostTitle()))
                  .append(',').append(csv(r.getPosterNickname()))
                  .append(',').append(csv(r.getReporterNickname()))
                  .append(',').append(csv(r.getReason().name()))
                  .append(',').append(csv(r.getDetail()))
                  .append(',').append(csv(r.getStatus().name()))
                  .append('\n');
            }
        }
        return csvResponse(sb.toString(), "reports_" + type + "_" + LocalDate.now() + ".csv");
    }

    private static ResponseEntity<byte[]> csvResponse(String content, String filename) {
        byte[] bom  = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] body = content.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + body.length];
        System.arraycopy(bom,  0, result, 0,          bom.length);
        System.arraycopy(body, 0, result, bom.length, body.length);

        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(result);
    }

    private static String csv(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String formatDt(LocalDateTime dt) {
        return dt == null ? "" : dt.format(DT_FMT);
    }
}
