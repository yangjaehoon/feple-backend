package com.feple.feple_backend.comment.service;

import com.feple.feple_backend.comment.dto.CommentLikeResult;
import com.feple.feple_backend.comment.dto.CommentResponseDto;
import com.feple.feple_backend.comment.dto.CreateCommentDto;
import com.feple.feple_backend.comment.dto.MyCommentResponseDto;

import java.util.List;

public interface CommentService {
    CommentResponseDto createComment(CreateCommentDto dto, Long userId);
    List<CommentResponseDto> getCommentsByPost(Long postId, Long userId);
    void deleteComment(Long commentId);
    void deleteOwnComment(Long commentId, Long requestUserId);
    CommentLikeResult toggleLike(Long commentId, Long userId);
    List<MyCommentResponseDto> getMyComments(Long userId);
    long countMyComments(Long userId);
}
