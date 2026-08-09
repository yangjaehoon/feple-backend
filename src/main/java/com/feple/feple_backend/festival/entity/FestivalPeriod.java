package com.feple.feple_backend.festival.entity;

import java.time.LocalDate;

/**
 * 페스티벌의 종료/진행중/예정 여부를 시작일·종료일만으로 판정하는 순수 규칙.
 * FestivalStatus(엔티티 필터링용)와 FestivalResponseDto(API 응답용)가 동일 규칙을
 * 각자 재구현하지 않도록 이 클래스로 일원화한다.
 */
public final class FestivalPeriod {
    private FestivalPeriod() {}

    public static boolean isEnded(LocalDate endDate, LocalDate today) {
        return endDate != null && endDate.isBefore(today);
    }

    public static boolean hasStarted(LocalDate startDate, LocalDate today) {
        return startDate != null && !startDate.isAfter(today);
    }

    public static boolean isOngoing(LocalDate startDate, LocalDate endDate, LocalDate today) {
        return !isEnded(endDate, today) && hasStarted(startDate, today);
    }

    public static boolean isUpcoming(LocalDate startDate, LocalDate today) {
        return startDate != null && startDate.isAfter(today);
    }
}
