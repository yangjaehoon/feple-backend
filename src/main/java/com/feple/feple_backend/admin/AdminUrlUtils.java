package com.feple.feple_backend.admin;

import com.feple.feple_backend.global.exception.InvalidRequestException;

import org.springframework.web.util.UriComponentsBuilder;

public final class AdminUrlUtils {

    private AdminUrlUtils() {}

    /**
     * 페이지네이션/리다이렉트용 쿼리스트링을 조립한다. key-value 쌍으로 넘기며, value가 null이거나
     * blank 문자열이면 해당 파라미터는 생략한다. UriComponentsBuilder를 통해 값을 URL 인코딩하므로
     * 필터값에 '&amp;'/'=' 등이 포함돼도 쿼리스트링이 깨지지 않는다.
     *
     * @return 선행 '?' 없는 쿼리스트링 (예: "targetType=USER&amp;from=2026-01-01"), 파라미터가 하나도 없으면 빈 문자열
     */
    public static String buildQueryString(Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new InvalidRequestException("key-value 쌍의 개수가 맞지 않습니다.");
        }
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = (String) keyValuePairs[i];
            Object value = keyValuePairs[i + 1];
            if (value == null) continue;
            if (value instanceof String s && s.isBlank()) continue;
            builder.queryParam(key, value);
        }
        return queryStringOf(builder);
    }

    /** 인코딩된 쿼리스트링을 선행 '?' 없이 반환한다. 파라미터가 하나도 없으면 빈 문자열. */
    public static String queryStringOf(UriComponentsBuilder builder) {
        String query = encoded(builder);
        return query.startsWith("?") ? query.substring(1) : query;
    }

    /**
     * UriComponentsBuilder를 인코딩된 URL 문자열로 변환한다.
     * encode() 없이 build()만 하면 keyword의 한글이 그대로 Location 헤더에 들어가
     * Tomcat이 "invalid header"로 판단해 리다이렉트 자체를 제거해버린다(빈 화면 원인).
     */
    public static String encoded(UriComponentsBuilder builder) {
        return builder.build().encode().toUriString();
    }

    /** value가 null이거나 blank가 아닐 때만 쿼리 파라미터로 추가한다. */
    public static UriComponentsBuilder appendIfPresent(UriComponentsBuilder builder, String key, String value) {
        if (value != null && !value.isBlank()) {
            builder.queryParam(key, value);
        }
        return builder;
    }
}
