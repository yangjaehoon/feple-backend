package com.feple.feple_backend.admin;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BindingResultUtilsTest {

    private static BindingResult br(String... messages) {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "obj");
        for (String msg : messages) {
            result.addError(new ObjectError("obj", msg));
        }
        return result;
    }

    // ── extractErrorMessages ──────────────────────────────────────────────────

    @Test
    void 에러_없으면_빈_리스트_반환() {
        assertThat(BindingResultUtils.extractErrorMessages(br())).isEmpty();
    }

    @Test
    void 에러_1개면_메시지_1개_반환() {
        assertThat(BindingResultUtils.extractErrorMessages(br("필수 항목입니다.")))
                .containsExactly("필수 항목입니다.");
    }

    @Test
    void 에러_여러개면_모두_반환() {
        List<String> msgs = BindingResultUtils.extractErrorMessages(br("오류1", "오류2", "오류3"));
        assertThat(msgs).containsExactly("오류1", "오류2", "오류3");
    }

    @Test
    void 에러_메시지_순서_유지() {
        List<String> msgs = BindingResultUtils.extractErrorMessages(br("첫번째", "두번째"));
        assertThat(msgs.get(0)).isEqualTo("첫번째");
        assertThat(msgs.get(1)).isEqualTo("두번째");
    }

    // ── firstError ────────────────────────────────────────────────────────────

    @Test
    void 에러_없으면_빈_문자열_반환() {
        assertThat(BindingResultUtils.firstError(br())).isEqualTo("");
    }

    @Test
    void 에러_1개면_해당_메시지_반환() {
        assertThat(BindingResultUtils.firstError(br("이름을 입력해주세요.")))
                .isEqualTo("이름을 입력해주세요.");
    }

    @Test
    void 에러_여러개면_첫번째_메시지만_반환() {
        assertThat(BindingResultUtils.firstError(br("첫번째 오류", "두번째 오류")))
                .isEqualTo("첫번째 오류");
    }
}
