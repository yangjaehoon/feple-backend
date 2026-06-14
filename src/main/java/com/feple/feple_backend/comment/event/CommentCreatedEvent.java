package com.feple.feple_backend.comment.event;

public record CommentCreatedEvent(
    Long postAuthorId,
    String commenterNickname,
    String postTitle,
    Long postId
) {}
