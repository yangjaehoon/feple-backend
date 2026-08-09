package com.feple.feple_backend.festival.entity;

/**
 * 기상청 초단기예보 PTY(강수형태) 코드.
 * 선언 순서가 곧 코드 값 순서(0~4)와 같아야 한다 — WeatherService가 하루치 예보 중
 * "가장 심한 강수 형태"를 고를 때 이 순서(ordinal)로 비교한다.
 */
public enum PtyCode {
    NONE("0"),
    RAIN("1"),
    RAIN_SNOW("2"),
    SNOW("3"),
    SHOWER("4");

    private final String code;

    PtyCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static PtyCode fromCode(String code) {
        for (PtyCode value : values()) {
            if (value.code.equals(code)) return value;
        }
        return NONE;
    }
}
