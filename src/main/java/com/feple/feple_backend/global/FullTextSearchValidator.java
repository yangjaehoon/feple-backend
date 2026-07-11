package com.feple.feple_backend.global;

// MySQL innodb_ft_min_token_size 기본값(3) — 이보다 짧은 단어는 FULLTEXT 인덱스
// 자체에서 제외되어 "IU"처럼 짧은 키워드는 MATCH...AGAINST로 찾을 수 없다.
// 이 길이 미만인 키워드는 LIKE 기반 폴백 쿼리를 사용해야 한다.
public final class FullTextSearchSupport {

    private static final int MIN_TOKEN_SIZE = 3;

    private FullTextSearchSupport() {}

    public static boolean isTooShortForFullText(String trimmedKeyword) {
        return trimmedKeyword.length() < MIN_TOKEN_SIZE;
    }
}
