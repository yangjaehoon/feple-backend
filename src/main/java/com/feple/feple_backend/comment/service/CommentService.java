package com.feple.feple_backend.comment.service;

import com.feple.feple_backend.comment.dto.CommentLikeResult;
import com.feple.feple_backend.comment.dto.CommentResponseDto;
import com.feple.feple_backend.comment.dto.CreateCommentDto;
import com.feple.feple_backend.comment.dto.MyCommentResponseDto;

import java.util.List;
import java.util.Map;

public interface CommentService {
    CommentResponseDto createComment(CreateCommentDto dto, Long userId);
    /** @param sort "best"면 최상위 댓글을 좋아요순으로, 그 외(null 포함)는 작성순으로 정렬 */
    List<CommentResponseDto> getCommentsByPost(Long postId, Long userId, String sort);
    List<CommentResponseDto> getAdminCommentsByPost(Long postId, int limit);
    void deleteComment(Long commentId);
    void deleteOwnComment(Long commentId, Long requestUserId);
    void deleteByPostIds(List<Long> postIds);
    /** 회원 완전 삭제(hardDelete) — 이 유저가 쓴 모든 댓글을 자식(좋아요·신고)까지 물리 삭제 */
    void purgeAuthoredCommentsByUser(Long userId);
    /**
     * 회원 완전 삭제(hardDelete) — 다른 유저의 댓글이 이 유저를 멘션하고 있으면 users FK 때문에
     * 행을 지울 수 없으므로 멘션 참조만 끊는다(댓글 본문은 유지).
     */
    void clearMentionsByUser(Long userId);
    /** 회원 완전 삭제(hardDelete) 규모 검사용 — 소프트 삭제·블라인드 포함 이 유저가 쓴 댓글 총수. */
    long countAllCommentsByUser(Long userId);
    CommentLikeResult toggleLike(Long commentId, Long userId);
    List<MyCommentResponseDto> getMyComments(Long userId);
    List<MyCommentResponseDto> getRecentCommentsByUser(Long userId, int limit);
    long countMyComments(Long userId);
    void updateOwnComment(Long commentId, Long requestUserId, String content);
    long countCommentsContaining(String word);
    Map<Long, Long> getCommentCountsByUserIds(List<Long> userIds);
    /** 회원 탈퇴 시 해당 유저의 댓글 좋아요 데이터 일괄 제거 */
    void removeLikesByUser(Long userId);
}
