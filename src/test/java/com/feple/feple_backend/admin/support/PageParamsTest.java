package com.feple.feple_backend.admin.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageParamsTest {

    @Test
    void null이면_0() {
        assertThat(new PageParams(null).page()).isZero();
    }

    @Test
    void 음수면_0으로_정규화() {
        assertThat(new PageParams(-1).page()).isZero();
    }

    @Test
    void 양수는_그대로() {
        assertThat(new PageParams(3).page()).isEqualTo(3);
    }
}
