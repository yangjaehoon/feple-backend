package com.feple.feple_backend.post.event;

public record PostDeletedByAdminEvent(
        Long postAuthorId,
        String postTitle
) {}
