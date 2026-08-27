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
        String query = builder.build().encode().toUriString();
        return query.startsWith("?") ? query.substring(1) : query;
    }
}
