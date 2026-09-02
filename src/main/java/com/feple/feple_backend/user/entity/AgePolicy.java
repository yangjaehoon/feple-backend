package com.feple.feple_backend.user.entity;

import java.time.LocalDate;
import java.time.Period;

/**
 * 커뮤니티 서비스 이용 최소 연령 규칙. Apple App Store 심사 가이드라인 5.1.1(아동 개인정보)
 * 대응 — 만 {@value #MINIMUM_AGE}세 미만은 계정 생성이 차단되고 수집된 개인정보가 파기된다.
 * (한국 개인정보보호법상 만 14세 미만 법정대리인 동의 요건과도 일치)
 */
public final class AgePolicy {

    /** 이용 가능 최소 만 나이. */
    public static final int MINIMUM_AGE = 14;

    private AgePolicy() {}

    public static int ageOn(LocalDate birthDate, LocalDate today) {
        return Period.between(birthDate, today).getYears();
    }

    /** 오늘 기준으로 최소 연령을 충족하는지. */
    public static boolean meetsMinimumAge(LocalDate birthDate, LocalDate today) {
        return birthDate != null && ageOn(birthDate, today) >= MINIMUM_AGE;
    }
}
