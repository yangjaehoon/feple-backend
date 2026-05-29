package com.feple.feple_backend.post.entity;

import java.util.Optional;

public enum BoardType {
    FREE,
    MATE,
    FESTIVAL_COMPANION,
    FESTIVAL_TICKET;

    /** 관리자 필터 문자열 → BoardType. ARTIST/FESTIVAL 같은 비-enum 필터는 empty 반환. */
    public static Optional<BoardType> fromAdminFilter(String filter) {
        if (filter == null || filter.isBlank()) return Optional.empty();
        try {
            return Optional.of(BoardType.valueOf(filter));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
