package com.feple.feple_backend.comment.event;

public record CommentCreatedEvent(
    Long postAuthorId,
    String commenterNickname,
    String postTitle,
    Long postId,
    Long mentionedUserId,  // 답글로 실제 지목된 댓글의 작성자 — null이면 최상위 댓글이거나 본인/게시글 작성자에게 답글
    Long commenterId             // 포인트 지급 대상
) {}
