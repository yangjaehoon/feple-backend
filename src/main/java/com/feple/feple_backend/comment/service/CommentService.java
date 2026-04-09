package com.feple.feple_backend.comment.service;

import com.feple.feple_backend.comment.dto.CreateCommentDto;
import com.feple.feple_backend.comment.dto.CommentResponseDto;

import java.util.List;

public interface CommentService {
    CommentResponseDto createComment(CreateCommentDto dto, Long userId);
    List<CommentResponseDto> getCommentsByPost(Long postId);
    void deleteComment(Long commentId);
    void deleteOwnComment(Long commentId, Long requestUserId);
}
