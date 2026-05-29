package com.feple.feple_backend.admin;

import com.feple.feple_backend.comment.entity.CommentReport;
import com.feple.feple_backend.comment.service.CommentReportService;
import com.feple.feple_backend.post.entity.PostReport;
import com.feple.feple_backend.post.service.PostReportService;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/export")
@RequiredArgsConstructor
public class AdminCsvController {

    private final UserAdminService userAdminService;
    private final PostReportService postReportService;
    private final CommentReportService commentReportService;

    @GetMapping("/users.csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportUsers() {
        List<UserResponseDto> users = userAdminService.getAllUsersForExport();
        StringBuilder sb = new StringBuilder("ID,닉네임,이메일,역할,가입일,정지여부\n");
        for (UserResponseDto u : users) {
            sb.append(CsvUtils.cell(u.getId()))
              .append(',').append(CsvUtils.cell(u.getNickname()))
              .append(',').append(CsvUtils.cell(u.getEmail()))
              .append(',').append(CsvUtils.cell(u.getRole().getDisplayName()))
              .append(',').append(CsvUtils.cell(CsvUtils.formatDt(u.getCreatedAt())))
              .append(',').append(CsvUtils.cell(u.isBanned() ? (u.isPermanentBan() ? "영구정지" : "정지중") : ""))
              .append('\n');
        }
        return CsvUtils.response(sb.toString(), "users_" + LocalDate.now() + ".csv");
    }

    @GetMapping("/reports.csv")
    @ResponseBody
    public ResponseEntity<byte[]> exportReports(@RequestParam(defaultValue = "post") String type) {
        StringBuilder sb = new StringBuilder();
        if ("comment".equals(type)) {
            sb.append("ID,신고일시,댓글ID,댓글내용,게시글제목,댓글작성자,신고자,사유,상세,상태\n");
            for (CommentReport r : commentReportService.getAllCommentReportsForExport()) {
                sb.append(CsvUtils.cell(r.getId()))
                  .append(',').append(CsvUtils.cell(CsvUtils.formatDt(r.getCreatedAt())))
                  .append(',').append(CsvUtils.cell(r.getCommentId()))
                  .append(',').append(CsvUtils.cell(r.getCommentContent()))
                  .append(',').append(CsvUtils.cell(r.getCommentPostTitle()))
                  .append(',').append(CsvUtils.cell(r.getCommentUserNickname()))
                  .append(',').append(CsvUtils.cell(r.getReporterNickname()))
                  .append(',').append(CsvUtils.cell(r.getReason().name()))
                  .append(',').append(CsvUtils.cell(r.getDetail()))
                  .append(',').append(CsvUtils.cell(r.getStatus().name()))
                  .append('\n');
            }
        } else {
            sb.append("ID,신고일시,게시글ID,게시글제목,게시자,신고자,사유,상세,상태\n");
            for (PostReport r : postReportService.getAllPostReportsForExport()) {
                sb.append(CsvUtils.cell(r.getId()))
                  .append(',').append(CsvUtils.cell(CsvUtils.formatDt(r.getCreatedAt())))
                  .append(',').append(CsvUtils.cell(r.getPostId()))
                  .append(',').append(CsvUtils.cell(r.getPostTitle()))
                  .append(',').append(CsvUtils.cell(r.getPosterNickname()))
                  .append(',').append(CsvUtils.cell(r.getReporterNickname()))
                  .append(',').append(CsvUtils.cell(r.getReason().name()))
                  .append(',').append(CsvUtils.cell(r.getDetail()))
                  .append(',').append(CsvUtils.cell(r.getStatus().name()))
                  .append('\n');
            }
        }
        return CsvUtils.response(sb.toString(), "reports_" + type + "_" + LocalDate.now() + ".csv");
    }
}
