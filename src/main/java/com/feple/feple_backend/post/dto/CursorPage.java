package com.feple.feple_backend.post.dto;

import java.util.List;

public record CursorPage<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext
) {}
