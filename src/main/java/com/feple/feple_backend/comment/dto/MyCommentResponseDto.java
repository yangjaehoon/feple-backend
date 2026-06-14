package com.feple.feple_backend.comment.dto;

import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.post.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MyCommentResponseDto {
    private Long commentId;
    private String content;
    private Long postId;
    private String postTitle;
    private String postContent;
    private String postNickname;
    private int postLikeCount;
    private String boardDisplayName;
    private LocalDateTime createdAt;

    public static MyCommentResponseDto from(Comment comment) {
        Post post = comment.getPost();
        return new MyCommentResponseDto(
                comment.getId(),
                comment.getContent(),
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthorNickname(),
                post.getLikeCount(),
                post.getDisplayBoardName(),
                comment.getCreatedAt()
        );
    }
}
