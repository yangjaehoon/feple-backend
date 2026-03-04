package com.feple.feple_backend.dto.comment;

import com.feple.feple_backend.domain.comment.Comment;
import com.feple.feple_backend.domain.post.BoardType;
import com.feple.feple_backend.domain.post.Post;
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

    public static MyCommentResponseDto from(Comment comment) {
        Post post = comment.getPost();

        String boardDisplayName;
        if (post.getArtist() != null) {
            boardDisplayName = post.getArtist().getName() + " 게시판";
        } else if (post.getFestival() != null) {
            boardDisplayName = post.getFestival().getTitle() + " 게시판";
        } else if (post.getBoardType() == BoardType.FREE) {
            boardDisplayName = "자유 게시판";
        } else if (post.getBoardType() == BoardType.MATE) {
            boardDisplayName = "동행 게시판";
        } else {
            boardDisplayName = "게시판";
        }

        return new MyCommentResponseDto(
                comment.getId(),
                comment.getContent(),
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getUser().getNickname(),
                post.getLikeCount(),
                boardDisplayName
        );
    }
}
