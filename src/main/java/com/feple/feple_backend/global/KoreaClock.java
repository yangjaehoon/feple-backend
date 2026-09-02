package com.feple.feple_backend.global;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/**
 * 한국 표준시(Asia/Seoul) 기준 오늘 날짜/현재 시각을 제공한다.
 *
 * <p>정적 유틸이 아니라 주입 가능한 빈이다 — 테스트에서 {@code new KoreaClock(Clock.fixed(...))}로
 * 시간을 고정할 수 있게 하기 위함(이전에는 mockStatic으로 우회했다).
 */
@Component
public class KoreaClock {

    // 문자열 상수를 별도로 두는 이유: @Scheduled(zone = ...)는 컴파일 타임 상수만 받으므로
    // ZoneId 객체(ZONE)를 쓸 수 없다. 앱 전역의 "운영 타임존"을 이 한 곳에서만 정의한다.
    public static final String ZONE_ID = "Asia/Seoul";

    public static final ZoneId ZONE = ZoneId.of(ZONE_ID);

    private final Clock clock;

    public KoreaClock() {
        this(Clock.system(ZONE));
    }

    public KoreaClock(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalTime now() {
        return LocalTime.now(clock);
    }
}
