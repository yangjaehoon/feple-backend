package com.feple.feple_backend.global;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/** 한국 표준시(Asia/Seoul) 기준 오늘 날짜/현재 시각을 구하는 공용 유틸. */
public final class KoreaClock {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private KoreaClock() {}

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalTime now() {
        return LocalTime.now(ZONE);
    }
}
