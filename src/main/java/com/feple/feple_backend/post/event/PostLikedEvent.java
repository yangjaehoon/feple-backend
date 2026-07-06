package com.feple.feple_backend.post.event;

public record PostLikedEvent(
        Long postAuthorId,
        String likerNickname,
        String postTitle,
        Long postId,
        Long likerId
) {}
