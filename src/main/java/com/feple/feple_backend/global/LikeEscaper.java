package com.feple.feple_backend.global;

/**
 * JPQL LIKE 쿼리에 사용할 이스케이프 처리 유틸리티.
 * 이스케이프 문자로 '!'를 사용하며, @Query에 ESCAPE '!' 를 함께 명시해야 한다.
 */
public final class LikeEscaper {

    private LikeEscaper() {}

    public static String escape(String keyword) {
        if (keyword == null) return null;
        return keyword
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }

    /** null 또는 공백이면 "", 아니면 trim 후 escape. */
    public static String escapeOrEmpty(String keyword) {
        return (keyword == null || keyword.isBlank()) ? "" : escape(keyword.trim());
    }

    /** null 또는 공백이면 null, 아니면 trim 후 escape. */
    public static String escapeOrNull(String keyword) {
        return (keyword == null || keyword.isBlank()) ? null : escape(keyword.trim());
    }
}
