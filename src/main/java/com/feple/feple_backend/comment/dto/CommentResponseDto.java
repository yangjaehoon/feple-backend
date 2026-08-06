package com.feple.feple_backend.comment.dto;

import com.feple.feple_backend.comment.entity.Comment;
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
    private Long mentionedUserId;
    private String mentionedNickname;

    public static CommentResponseDto from(Comment comment, boolean certified, boolean liked) {
        boolean anon = comment.isAnonymous();
        return CommentResponseDto.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .userId(comment.getUserId())
                .nickname(anon ? "익명" : comment.getUserNickname())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .certified(anon ? false : certified)
                .userRole(anon ? null : comment.getUserRole())
                .parentId(comment.getParentId())
                .likeCount(comment.getLikeCount())
                .liked(liked)
                .profileImageUrl(anon ? null : comment.getUserProfileImageUrl())
                .anonymous(anon)
                .blinded(comment.isBlinded())
                .mentionedUserId(comment.getMentionedUserId())
                .mentionedNickname(comment.getMentionedNickname())
                .build();
    }
}
