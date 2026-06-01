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
}
