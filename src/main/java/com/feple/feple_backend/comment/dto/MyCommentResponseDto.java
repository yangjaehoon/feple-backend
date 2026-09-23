package com.feple.feple_backend.comment.dto;

import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.global.AnonymousAuthorVisibility;
import com.feple.feple_backend.post.entity.Post;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
        // 게시글이 익명 글이면 댓글을 단 사람의 "내 댓글" 목록에서도 작성자 실명을
        // 노출하면 안 된다 — 익명 글에 댓글을 달았다는 이유만으로 작성자 신원을 알 수 있게 됨.
        String postNickname = post.isAnonymous()
                ? AnonymousAuthorVisibility.ANONYMOUS_NICKNAME : post.getAuthorNickname();
        return new MyCommentResponseDto(
                comment.getId(),
                comment.getContent(),
                comment.getPostId(),
                comment.getPostTitle(),
                post.getContent(),
                postNickname,
                post.getLikeCount(),
                post.getBoardDisplayName(),
                comment.getCreatedAt()
        );
    }
}
