package com.feple.feple_backend.festival.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class FestivalPeriodTest {

    private final LocalDate today = LocalDate.of(2026, 8, 10);

    @Test
    void 종료일이_오늘보다_이전이면_종료() {
        assertThat(FestivalPeriod.isEnded(today.minusDays(1), today)).isTrue();
    }

    @Test
    void 종료일이_오늘이면_아직_종료_아님() {
        assertThat(FestivalPeriod.isEnded(today, today)).isFalse();
    }

    @Test
    void 종료일이_없으면_종료_아님() {
        assertThat(FestivalPeriod.isEnded(null, today)).isFalse();
    }

    @Test
    void 시작일이_오늘_이전이고_종료일이_이후면_진행중() {
        assertThat(FestivalPeriod.isOngoing(today.minusDays(1), today.plusDays(1), today)).isTrue();
    }

    @Test
    void 아직_시작하지_않았으면_진행중_아님() {
        assertThat(FestivalPeriod.isOngoing(today.plusDays(1), today.plusDays(5), today)).isFalse();
    }

    @Test
    void 이미_종료됐으면_진행중_아님() {
        assertThat(FestivalPeriod.isOngoing(today.minusDays(5), today.minusDays(1), today)).isFalse();
    }

    @Test
    void 시작일이_오늘보다_이후면_예정() {
        assertThat(FestivalPeriod.isUpcoming(today.plusDays(1), today)).isTrue();
    }

    @Test
    void 시작일이_오늘이면_예정_아님() {
        assertThat(FestivalPeriod.isUpcoming(today, today)).isFalse();
    }
}
