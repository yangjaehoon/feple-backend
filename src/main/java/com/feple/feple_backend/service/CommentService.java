package com.feple.feple_backend.service;

import com.feple.feple_backend.dto.comment.CreateCommentDto;
import com.feple.feple_backend.dto.comment.CommentResponseDto;

import java.util.List;

public interface CommentService {
    CommentResponseDto createComment(CreateCommentDto dto);
    List<CommentResponseDto> getCommentsByPost(Long postId);
    void deleteComment(Long commentId);
}
