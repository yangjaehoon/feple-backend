package com.feple.feple_backend.admin;

public final class AdminParamDefaults {

    private AdminParamDefaults() {}

    // page 파라미터 없이 접근 시 null → primitive int 변환 실패(400) 방지
    public static int orZero(Integer page) {
        return page == null ? 0 : page;
    }

    public static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    public static String orDefault(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static String orDefaultIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }
}
