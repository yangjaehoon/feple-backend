package com.feple.feple_backend.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AgePolicyTest {

    private final LocalDate today = LocalDate.of(2026, 9, 2);

    @Test
    void 만_14세_생일_당일이면_충족() {
        assertThat(AgePolicy.meetsMinimumAge(LocalDate.of(2012, 9, 2), today)).isTrue();
    }

    @Test
    void 만_14세_생일_하루_전이면_미충족() {
        assertThat(AgePolicy.meetsMinimumAge(LocalDate.of(2012, 9, 3), today)).isFalse();
    }

    @Test
    void 만_13세면_미충족() {
        assertThat(AgePolicy.meetsMinimumAge(LocalDate.of(2013, 1, 1), today)).isFalse();
    }

    @Test
    void 생년월일_null이면_미충족() {
        assertThat(AgePolicy.meetsMinimumAge(null, today)).isFalse();
    }
}
