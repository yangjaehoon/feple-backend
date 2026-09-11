package com.feple.feple_backend.appconfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import java.util.Map;

/**
 * {@code app_config.feature_flags} 컬럼에 저장되는 {@code {"키": boolean}} JSON 문자열의
 * 파싱·정규화를 한곳에서 담당한다. 조회 경로(깨진 값이면 빈 맵)와 저장 경로(깨진 값이면 검증 실패)가
 * 동일한 파싱 규칙을 공유하도록 하기 위한 유틸.
 */
public final class FeatureFlags {

    private static final TypeReference<Map<String, Boolean>> TYPE = new TypeReference<>() {};
    private static final String EMPTY_JSON_OBJECT = "{}";
    private static final String INVALID_MESSAGE =
            "기능 스위치는 올바른 JSON 형식이어야 합니다. 예: {\"chatEnabled\": true}";

    private FeatureFlags() {}

    /**
     * JSON 문자열을 {@code Map<String, Boolean>}으로 파싱한다. 비어 있으면 빈 맵,
     * 형식이 잘못됐으면 {@link InvalidRequestException}.
     */
    public static Map<String, Boolean> parse(ObjectMapper objectMapper, String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Boolean> parsed = objectMapper.readValue(json, TYPE);
            return parsed != null ? parsed : Map.of();
        } catch (JsonProcessingException e) {
            throw new InvalidRequestException(INVALID_MESSAGE);
        }
    }

    /** 관리자 입력을 검증한 뒤 표준 형태(키→boolean)로 다시 직렬화한다. 비어 있으면 {@code "{}"}. */
    public static String normalize(ObjectMapper objectMapper, String json) {
        Map<String, Boolean> parsed = parse(objectMapper, json);
        if (parsed.isEmpty()) {
            return EMPTY_JSON_OBJECT;
        }
        try {
            return objectMapper.writeValueAsString(parsed);
        } catch (JsonProcessingException e) {
            throw new InvalidRequestException(INVALID_MESSAGE);
        }
    }
}
