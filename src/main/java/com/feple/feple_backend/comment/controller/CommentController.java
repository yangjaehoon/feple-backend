package com.feple.feple_backend.comment.controller;

import com.feple.feple_backend.comment.dto.CommentResponseDto;
import com.feple.feple_backend.comment.dto.CreateCommentDto;
import com.feple.feple_backend.comment.service.CommentService;
import jakarta.validation.Valid;
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

    @PostMapping
    public ResponseEntity<CommentResponseDto> create(@Valid @RequestBody CreateCommentDto dto,
                                                     @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(commentService.createComment(dto, userId));
    }

    @GetMapping("/post/{PostId}")
    public ResponseEntity<List<CommentResponseDto>> list(@PathVariable Long PostId) {
        return ResponseEntity.ok(commentService.getCommentsByPost(PostId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal Long userId) {
        commentService.deleteOwnComment(id, userId);
        return ResponseEntity.noContent().build();
    }
}
