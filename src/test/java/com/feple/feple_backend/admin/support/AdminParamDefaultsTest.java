package com.feple.feple_backend.admin.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AdminParamDefaultsTest {

    @Test
    void pageOrFirst_null이면_0() {
        assertThat(AdminParamDefaults.pageOrFirst(null)).isZero();
    }

    @Test
    void pageOrFirst_값있으면_그대로() {
        assertThat(AdminParamDefaults.pageOrFirst(5)).isEqualTo(5);
    }

    @Test
    void pageOrFirst_음수면_첫페이지로_보정() {
        // 음수를 그대로 넘기면 PageRequest.of가 예외를 던져 목록 대신 400 페이지가 렌더된다
        assertThat(AdminParamDefaults.pageOrFirst(-1)).isZero();
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
