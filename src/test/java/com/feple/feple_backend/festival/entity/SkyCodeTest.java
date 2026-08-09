package com.feple.feple_backend.festival.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SkyCodeTest {

    @Test
    void 코드값으로_올바른_enum_반환() {
        assertThat(SkyCode.fromCode("1")).isEqualTo(SkyCode.SUNNY);
        assertThat(SkyCode.fromCode("3")).isEqualTo(SkyCode.CLOUDY);
        assertThat(SkyCode.fromCode("4")).isEqualTo(SkyCode.OVERCAST);
    }

    @Test
    void 알수없는_코드값은_SUNNY로_폴백() {
        assertThat(SkyCode.fromCode("9")).isEqualTo(SkyCode.SUNNY);
        assertThat(SkyCode.fromCode(null)).isEqualTo(SkyCode.SUNNY);
    }

    @Test
    void getCode는_원본_기상청_코드를_반환() {
        assertThat(SkyCode.SUNNY.getCode()).isEqualTo("1");
        assertThat(SkyCode.CLOUDY.getCode()).isEqualTo("3");
        assertThat(SkyCode.OVERCAST.getCode()).isEqualTo("4");
    }
}
