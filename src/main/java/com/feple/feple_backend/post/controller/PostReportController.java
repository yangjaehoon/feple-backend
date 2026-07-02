package com.feple.feple_backend.post.controller;

import com.feple.feple_backend.post.dto.SubmitReportCommand;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.post.service.PostReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "신고", description = "게시글 신고 제출")
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostReportController {

    private final PostReportService postReportService;

    @PostMapping("/{postId}/report")
    public ResponseEntity<Void> report(
            @PathVariable Long postId,
            @Valid @RequestBody ReportRequest body,
            @AuthenticationPrincipal Long userId) {
        postReportService.submitReport(postId, userId, new SubmitReportCommand(body.getReason(), body.getDetail()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Data
    public static class ReportRequest {
        @NotNull(message = "신고 사유를 선택해주세요.")
        private ReportReason reason;
        @Size(max = 500, message = "신고 사유는 500자 이하로 입력해주세요.")
        private String detail;
    }
}
