package com.feple.feple_backend.global;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JpqlLikeEscaperTest {

    @Test
    void escape_null이면_null_반환() {
        assertThat(JpqlLikeEscaper.escape(null)).isNull();
    }

    @Test
    void escape_특수문자_이스케이프() {
        assertThat(JpqlLikeEscaper.escape("100% off_sale!")).isEqualTo("100!% off!_sale!!");
    }

    @Test
    void escapeOrEmpty_null이면_빈문자열() {
        assertThat(JpqlLikeEscaper.escapeOrEmpty(null)).isEmpty();
    }

    @Test
    void escapeOrEmpty_공백이면_빈문자열() {
        assertThat(JpqlLikeEscaper.escapeOrEmpty("   ")).isEmpty();
    }

    @Test
    void escapeOrEmpty_값있으면_trim후_이스케이프() {
        assertThat(JpqlLikeEscaper.escapeOrEmpty("  100%  ")).isEqualTo("100!%");
    }

    @Test
    void escapeOrNull_null이면_null() {
        assertThat(JpqlLikeEscaper.escapeOrNull(null)).isNull();
    }

    @Test
    void escapeOrNull_공백이면_null() {
        assertThat(JpqlLikeEscaper.escapeOrNull("   ")).isNull();
    }

    @Test
    void escapeOrNull_값있으면_trim후_이스케이프() {
        assertThat(JpqlLikeEscaper.escapeOrNull("  키워드  ")).isEqualTo("키워드");
    }
}
