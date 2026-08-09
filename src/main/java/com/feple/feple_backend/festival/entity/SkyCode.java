package com.feple.feple_backend.festival.entity;

/** 기상청 초단기예보 SKY(하늘상태) 코드. */
public enum SkyCode {
    SUNNY("1"),
    CLOUDY("3"),
    OVERCAST("4");

    private final String code;

    SkyCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static SkyCode fromCode(String code) {
        for (SkyCode value : values()) {
            if (value.code.equals(code)) return value;
        }
        return SUNNY;
    }
}
