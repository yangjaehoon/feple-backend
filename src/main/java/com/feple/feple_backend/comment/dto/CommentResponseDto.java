package com.feple.feple_backend.comment.dto;

import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.user.entity.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
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

    public CommentResponseDto(Long id, Long postId, Long userId, String nickname,
                              String content, LocalDateTime createdAt, LocalDateTime updatedAt,
                              boolean certified, UserRole userRole, Long parentId,
                              int likeCount, boolean liked, String profileImageUrl, boolean anonymous) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.nickname = nickname;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.certified = certified;
        this.userRole = userRole;
        this.parentId = parentId;
        this.likeCount = likeCount;
        this.liked = liked;
        this.profileImageUrl = profileImageUrl;
        this.anonymous = anonymous;
    }

    public static CommentResponseDto from(Comment comment, boolean certified, boolean liked) {
        boolean anon = comment.isAnonymous();
        return new CommentResponseDto(
                comment.getId(),
                comment.getPostId(),
                comment.getUserId(),
                anon ? "익명" : comment.getUserNickname(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                anon ? false : certified,
                anon ? null : comment.getUserRole(),
                comment.getParentId(),
                comment.getLikeCount(),
                liked,
                anon ? null : comment.getUserProfileImageUrl(),
                anon
        );
    }
}
