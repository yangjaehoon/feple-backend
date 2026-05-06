package com.feple.feple_backend.post.controller;

import com.feple.feple_backend.post.entity.ReportReason;
import com.feple.feple_backend.post.service.PostReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
        postReportService.submitReport(postId, userId, body.getReason(), body.getDetail());
        return ResponseEntity.ok().build();
    }

    @Data
    public static class ReportRequest {
        @NotNull
        private ReportReason reason;
        private String detail;
    }
}
