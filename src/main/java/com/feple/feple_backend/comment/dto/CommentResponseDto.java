package com.feple.feple_backend.comment.dto;

import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.user.entity.UserRole;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponseDto {
    private Long id;
    private Long postId;
    private Long userId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean certified;
    private UserRole userRole;
    private Long parentId;
    private int likeCount;
    private boolean liked;
    private String profileImageUrl;
    private boolean anonymous;
    private boolean blinded;
    private boolean edited;
    private Long mentionedUserId;
    private String mentionedNickname;

    // 조회자 관점에 따라 달라지는 값(본인 인증 뱃지 표시 여부, 좋아요 여부)을 묶어
    // from()의 파라미터를 3개 이하로 유지한다.
    public record ViewerContext(boolean certified, boolean liked) {}

    public static CommentResponseDto from(Comment comment, ViewerContext viewer, FileStorageService fileStorageService) {
        boolean anon = comment.isAnonymous();
        return CommentResponseDto.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .nickname(anon ? "익명" : comment.getUserNickname())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .certified(anon ? false : viewer.certified())
                .userRole(anon ? null : comment.getUserRole())
                .parentId(comment.getParentId())
                .likeCount(comment.getLikeCount())
                .liked(viewer.liked())
                .profileImageUrl(anon ? null : fileStorageService.resolveProfileImageUrl(comment.getUserProfileImageUrl()))
                .anonymous(anon)
                .blinded(comment.isBlinded())
                .edited(comment.isEdited())
                .mentionedUserId(comment.getMentionedUserId())
                .mentionedNickname(comment.getMentionedNickname())
                .build();
    }
}
