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
        return toQueryString(applyParams(UriComponentsBuilder.newInstance(), keyValuePairs));
    }

    /**
     * basePath에 key-value 파라미터를 붙여 인코딩된 URL을 만든다. value가 null이거나 blank면 생략한다.
     * 목록/상세 화면의 "돌아가기" 링크 조립용. redirect: 접두사가 필요하면 {@link AdminActionUtils#listRedirect}를 쓴다.
     */
    public static String listUrl(String basePath, Object... keyValuePairs) {
        return toEncodedString(applyParams(UriComponentsBuilder.fromPath(basePath), keyValuePairs));
    }

    /** 선행 '?' 없는 인코딩된 쿼리스트링을 반환한다. 파라미터가 하나도 없으면 빈 문자열. */
    public static String toQueryString(UriComponentsBuilder builder) {
        String query = toEncodedString(builder);
        return query.startsWith("?") ? query.substring(1) : query;
    }

    /**
     * UriComponentsBuilder를 인코딩된 URL 문자열로 변환한다.
     * encode() 없이 build()만 하면 keyword의 한글이 그대로 Location 헤더에 들어가
     * Tomcat이 "invalid header"로 판단해 리다이렉트 자체를 제거해버린다(빈 화면 원인).
     */
    public static String toEncodedString(UriComponentsBuilder builder) {
        return builder.build().encode().toUriString();
    }

    /** value에 실제 텍스트가 있을 때만(null·공백 아님) 쿼리 파라미터로 추가한다. */
    public static void appendIfHasText(UriComponentsBuilder builder, String key, String value) {
        if (value != null && !value.isBlank()) {
            builder.queryParam(key, value);
        }
    }

    private static UriComponentsBuilder applyParams(UriComponentsBuilder builder, Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new InvalidRequestException("key-value 쌍의 개수가 맞지 않습니다.");
        }
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            String key = (String) keyValuePairs[i];
            Object value = keyValuePairs[i + 1];
            if (value == null) continue;
            if (value instanceof String s && s.isBlank()) continue;
            builder.queryParam(key, value);
        }
        return builder;
    }
}
