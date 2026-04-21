package com.feple.feple_backend.comment.dto;

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
    private boolean certified;

    public CommentResponseDto(Long id, Long postId, Long userId, String nickname,
                              String content, LocalDateTime createdAt, boolean certified) {
        this.id = id;
        this.postId = postId;
        this.userId = userId;
        this.nickname = nickname;
        this.content = content;
        this.createdAt = createdAt;
        this.certified = certified;
    }
}
