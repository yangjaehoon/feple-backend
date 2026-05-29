package com.feple.feple_backend.post.controller;

import com.feple.feple_backend.post.dto.SubmitReportCommand;
import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.post.service.PostReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
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
        return ResponseEntity.ok().build();
    }

    @Data
    public static class ReportRequest {
        @NotNull
        private ReportReason reason;
        private String detail;
    }
}
