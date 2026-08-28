package com.feple.feple_backend.post.controller;

import com.feple.feple_backend.post.dto.ReportSubmitRequest;
import com.feple.feple_backend.post.service.PostReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "신고", description = "게시글 신고 제출")
@RestController
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostReportController {

    private final PostReportService postReportService;

    @PostMapping("/{postId}/report")
    public ResponseEntity<Void> report(
            @PathVariable Long postId,
            @Valid @RequestBody ReportSubmitRequest body,
            @AuthenticationPrincipal Long userId) {
        postReportService.submitReport(postId, userId, body);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
