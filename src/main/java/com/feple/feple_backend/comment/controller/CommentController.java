package com.feple.feple_backend.comment.controller;

import com.feple.feple_backend.comment.dto.CommentResponseDto;
import com.feple.feple_backend.comment.dto.CreateCommentDto;
import com.feple.feple_backend.comment.service.CommentReportService;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.post.entity.ReportReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final CommentReportService commentReportService;

    @PostMapping
    public ResponseEntity<CommentResponseDto> create(@Valid @RequestBody CreateCommentDto dto,
                                                     @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(commentService.createComment(dto, userId));
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentResponseDto>> list(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal Long userId) {
        commentService.deleteOwnComment(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{commentId}/report")
    public ResponseEntity<Void> report(
            @PathVariable Long commentId,
            @Valid @RequestBody ReportRequest body,
            @AuthenticationPrincipal Long userId) {
        commentReportService.submitReport(commentId, userId, body.getReason(), body.getDetail());
        return ResponseEntity.ok().build();
    }

    @Data
    public static class ReportRequest {
        @NotNull
        private ReportReason reason;
        private String detail;
    }
}
