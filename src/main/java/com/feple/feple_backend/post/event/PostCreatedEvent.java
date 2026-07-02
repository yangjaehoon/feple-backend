package com.feple.feple_backend.post.event;

public record PostCreatedEvent(Long authorId, Long postId) {}
