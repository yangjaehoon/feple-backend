package com.feple.feple_backend.festival.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PtyCodeTest {

    @Test
    void 코드값으로_올바른_enum_반환() {
        assertThat(PtyCode.fromCode("0")).isEqualTo(PtyCode.NONE);
        assertThat(PtyCode.fromCode("1")).isEqualTo(PtyCode.RAIN);
        assertThat(PtyCode.fromCode("2")).isEqualTo(PtyCode.RAIN_SNOW);
        assertThat(PtyCode.fromCode("3")).isEqualTo(PtyCode.SNOW);
        assertThat(PtyCode.fromCode("4")).isEqualTo(PtyCode.SHOWER);
    }

    @Test
    void 알수없는_코드값은_NONE으로_폴백() {
        assertThat(PtyCode.fromCode("9")).isEqualTo(PtyCode.NONE);
        assertThat(PtyCode.fromCode(null)).isEqualTo(PtyCode.NONE);
    }

    @Test
    void 선언_순서가_코드_순서와_일치해야_심한_강수형태_비교가_정확함() {
        // WeatherService가 next.ordinal() > current.ordinal()로 "더 심한 강수"를 고르므로
        // 이 순서가 깨지면 최댓값 선택 로직이 조용히 잘못된 값을 고르게 된다.
        assertThat(PtyCode.NONE.ordinal()).isLessThan(PtyCode.RAIN.ordinal());
        assertThat(PtyCode.RAIN.ordinal()).isLessThan(PtyCode.RAIN_SNOW.ordinal());
        assertThat(PtyCode.RAIN_SNOW.ordinal()).isLessThan(PtyCode.SNOW.ordinal());
        assertThat(PtyCode.SNOW.ordinal()).isLessThan(PtyCode.SHOWER.ordinal());
    }
}
