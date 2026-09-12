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

    /** 아티스트/페스티벌 게시판이 아닌 경우의 게시판 표시명 (Post.getBoardDisplayName 참고). */
    public String displayName() {
        return switch (this) {
            case FREE -> "자유 게시판";
            case MATE -> "동행 게시판";
            default -> "게시판";
        };
    }
}
