package com.feple.feple_backend.badword;

import com.feple.feple_backend.badword.repository.BadWordRepository;
import com.feple.feple_backend.global.exception.BadWordException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BadWordFilterTest {

    @Mock BadWordRepository badWordRepository;
    @InjectMocks BadWordFilter filter;

    private void loadWords(String... words) {
        given(badWordRepository.findAllWords()).willReturn(List.of(words));
        filter.reload();
    }

    // ── validate ─────────────────────────────────────────────────────

    @Test
    void 금칙어_포함된_텍스트_예외() {
        loadWords("욕설");

        assertThatThrownBy(() -> filter.validate("이건 욕설이야"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("금칙어");
    }

    @Test
    void 금칙어_없는_텍스트_통과() {
        loadWords("욕설");

        assertThatNoException().isThrownBy(() -> filter.validate("깨끗한 텍스트입니다"));
    }

    @Test
    void null_텍스트_통과() {
        loadWords("욕설");

        assertThatNoException().isThrownBy(() -> filter.validate((String) null));
    }

    @Test
    void 금칙어_목록_비어있으면_검사_생략() {
        loadWords();

        assertThatNoException().isThrownBy(() -> filter.validate("욕설 포함해도 통과"));
    }

    @Test
    void 대소문자_구분_없이_검출() {
        loadWords("badword");

        assertThatThrownBy(() -> filter.validate("This has BADWORD in it"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 공백_제거_후_검출() {
        loadWords("욕설");

        assertThatThrownBy(() -> filter.validate("욕 설"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 여러_텍스트_중_하나라도_금칙어_포함시_예외() {
        loadWords("욕설");

        assertThatThrownBy(() -> filter.validate("깨끗한 텍스트", "이건 욕설이야", "또 깨끗한 텍스트"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── validateField ────────────────────────────────────────────────

    @Test
    void validateField_금칙어_포함시_BadWordException_필드명_일치() {
        loadWords("욕설");

        assertThatThrownBy(() -> filter.validateField("nickname", "욕설닉네임"))
                .isInstanceOf(BadWordException.class)
                .satisfies(e -> assertThat(((BadWordException) e).getField()).isEqualTo("nickname"));
    }

    @Test
    void validateField_null_텍스트_통과() {
        loadWords("욕설");

        assertThatNoException().isThrownBy(() -> filter.validateField("nickname", null));
    }

    @Test
    void validateField_금칙어_목록_비어있으면_검사_생략() {
        loadWords();

        assertThatNoException().isThrownBy(() -> filter.validateField("bio", "욕설 있어도 통과"));
    }
}
