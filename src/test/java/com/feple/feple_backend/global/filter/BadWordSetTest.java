package com.feple.feple_backend.global.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class BadWordSetTest {

    @Test
    void load_전에는_항상_false() {
        BadWordSet set = new BadWordSet();

        assertThat(set.contains("아무말")).isFalse();
    }

    @Test
    void 정확히_포함된_금칙어_감지() {
        BadWordSet set = new BadWordSet();
        set.load(List.of("바보"));

        assertThat(set.contains("너는 바보야")).isTrue();
    }

    @Test
    void 구두점으로_우회하려_해도_감지() {
        BadWordSet set = new BadWordSet();
        set.load(List.of("바보"));

        assertThat(set.contains("바.보")).isTrue();
        assertThat(set.contains("바-보")).isTrue();
        assertThat(set.contains("바 보")).isTrue();
    }

    @Test
    void 대소문자_구분없이_감지() {
        BadWordSet set = new BadWordSet();
        set.load(List.of("bad"));

        assertThat(set.contains("this is BAD word")).isTrue();
    }

    @Test
    void 금칙어_아니면_false() {
        BadWordSet set = new BadWordSet();
        set.load(List.of("바보"));

        assertThat(set.contains("정상적인 텍스트")).isFalse();
    }

    @Test
    void load_재호출시_이전_목록_대체() {
        BadWordSet set = new BadWordSet();
        set.load(List.of("바보"));

        set.load(List.of("멍청이"));

        assertThat(set.contains("바보")).isFalse();
        assertThat(set.contains("멍청이")).isTrue();
    }
}
