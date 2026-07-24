package com.feple.feple_backend.post.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record CursorPage<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext
) {
    public static int toPage(Long cursor) {
        return cursor == null ? 0 : cursor.intValue();
    }

    public static <T> CursorPage<T> of(Page<?> result, List<T> content, Long cursor) {
        boolean hasNext = result.hasNext();
        Long nextCursor = hasNext ? (long) (toPage(cursor) + 1) : null;
        return new CursorPage<>(content, nextCursor, hasNext);
    }
}
