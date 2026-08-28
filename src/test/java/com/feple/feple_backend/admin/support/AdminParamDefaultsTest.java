package com.feple.feple_backend.admin.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminParamDefaultsTest {

    @Test
    void orZero_null이면_0() {
        assertThat(AdminParamDefaults.orZero(null)).isZero();
    }

    @Test
    void orZero_값있으면_그대로() {
        assertThat(AdminParamDefaults.orZero(5)).isEqualTo(5);
    }

    @Test
    void orEmpty_null이면_빈문자열() {
        assertThat(AdminParamDefaults.orEmpty(null)).isEmpty();
    }

    @Test
    void orEmpty_값있으면_그대로() {
        assertThat(AdminParamDefaults.orEmpty("값")).isEqualTo("값");
    }

    @Test
    void orDefault_null이면_기본값() {
        assertThat(AdminParamDefaults.orDefault(null, "기본값")).isEqualTo("기본값");
    }

    @Test
    void orDefault_값있으면_그대로() {
        assertThat(AdminParamDefaults.orDefault("값", "기본값")).isEqualTo("값");
    }

    @Test
    void orDefaultIfBlank_null이면_기본값() {
        assertThat(AdminParamDefaults.orDefaultIfBlank(null, "기본값")).isEqualTo("기본값");
    }

    @Test
    void orDefaultIfBlank_공백이면_기본값() {
        assertThat(AdminParamDefaults.orDefaultIfBlank("   ", "기본값")).isEqualTo("기본값");
    }

    @Test
    void orDefaultIfBlank_값있으면_그대로() {
        assertThat(AdminParamDefaults.orDefaultIfBlank("값", "기본값")).isEqualTo("값");
    }
}
