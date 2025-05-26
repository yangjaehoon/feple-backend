package com.feple.feple_backend.controller;

import com.feple.feple_backend.dto.comment.CommentResponseDto;
import com.feple.feple_backend.dto.comment.CreateCommentDto;
import com.feple.feple_backend.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponseDto> create(@RequestBody CreateCommentDto dto) {
        return ResponseEntity.ok(commentService.createComment(dto));
    }

    @GetMapping("/post/{PostId}")
    public ResponseEntity<List<CommentResponseDto>> list(@PathVariable Long PostId) {
        return ResponseEntity.ok(commentService.getCommentsByPost(PostId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
