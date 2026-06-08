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
    List<MyCommentResponseDto> getRecentCommentsByUser(Long userId, int limit);
    long countMyComments(Long userId);
    void updateOwnComment(Long commentId, Long requestUserId, String content);
    long countCommentsContaining(String word);
    java.util.Map<Long, Long> getCommentCountsByUserIds(java.util.List<Long> userIds);
}
