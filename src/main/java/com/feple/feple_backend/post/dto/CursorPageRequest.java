package com.feple.feple_backend.post.dto;

public record CursorPageRequest(Long cursor, int size, Long viewerId) {
}
