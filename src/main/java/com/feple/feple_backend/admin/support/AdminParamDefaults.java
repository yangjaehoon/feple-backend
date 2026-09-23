package com.feple.feple_backend.admin.support;

import java.util.Set;

public final class AdminParamDefaults {

    private AdminParamDefaults() {}

    // page 파라미터 없이 접근 시 null → primitive int 변환 실패(400) 방지.
    // 음수 보정까지 포함한 pageOrFirst만 노출한다 — 이것만 쓰면 음수 page가 400으로 새어나간다.
    private static int orZero(Integer page) {
        return page == null ? 0 : page;
    }

    /**
     * 목록 화면의 page 파라미터 정규화 — 누락(null)이든 음수든 첫 페이지로 보정한다.
     *
     * <p>음수를 그대로 넘기면 {@code PageRequest.of}가 IllegalArgumentException을 던져
     * 목록 대신 400 에러 페이지가 렌더된다. 파라미터 record마다 orZero만 쓰거나
     * {@code Math.max(0, orZero(page))}를 각자 반복해 화면별로 동작이 달랐어서 하나로 합쳤다.
     */
    public static int pageOrFirst(Integer page) {
        return Math.max(0, orZero(page));
    }

    public static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    public static <T> Set<T> orEmptySet(Set<T> value) {
        return value == null ? Set.of() : value;
    }

    public static String orDefault(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static String orDefaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
