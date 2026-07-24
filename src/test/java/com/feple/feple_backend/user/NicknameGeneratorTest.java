package com.feple.feple_backend.user;

import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NicknameGeneratorTest {

    @Mock UserRepository userRepository;
    @Mock NicknameContentValidator nicknameContentValidator;

    private NicknameGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new NicknameGenerator(userRepository, nicknameContentValidator);
    }

    // ── sanitize ─────────────────────────────────────────────────────────

    @Test
    void sanitize_특수문자_제거후_검증통과시_그대로_반환() {
        String result = generator.sanitize("홍길동!!", "fallback");

        assertThat(result).isEqualTo("홍길동");
    }

    @Test
    void sanitize_정제후_2자미만이면_검증없이_fallback() {
        String result = generator.sanitize("a!", "fallback");

        assertThat(result).isEqualTo("fallback");
        verify(nicknameContentValidator, never()).validate(any());
    }

    @Test
    void sanitize_8자_초과면_8자로_자른뒤_검증() {
        String result = generator.sanitize("abcdefghij", "fallback");

        assertThat(result).isEqualTo("abcdefgh");
        verify(nicknameContentValidator).validate("abcdefgh");
    }

    @Test
    void sanitize_컨텐츠_검증_실패시_fallback_반환() {
        willThrow(new IllegalArgumentException("금칙어"))
                .given(nicknameContentValidator).validate("금칙어닉");

        String result = generator.sanitize("금칙어닉", "fallback");

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    void sanitize_특수문자만_있으면_fallback() {
        String result = generator.sanitize("!!!", "fallback");

        assertThat(result).isEqualTo("fallback");
    }

    // ── uniquify ─────────────────────────────────────────────────────────

    @Test
    void uniquify_미사용_닉네임이면_그대로_반환() {
        given(userRepository.existsByNickname("홍길동")).willReturn(false);

        assertThat(generator.uniquify("홍길동")).isEqualTo("홍길동");
    }

    @Test
    void uniquify_8자_초과면_잘라서_확인() {
        given(userRepository.existsByNickname("abcdefgh")).willReturn(false);

        assertThat(generator.uniquify("abcdefghij")).isEqualTo("abcdefgh");
    }

    @Test
    void uniquify_2자_미만이면_User로_대체() {
        given(userRepository.existsByNickname("User")).willReturn(false);

        assertThat(generator.uniquify("a")).isEqualTo("User");
    }

    @Test
    void uniquify_중복이면_숫자_suffix_부여() {
        given(userRepository.existsByNickname("홍길동")).willReturn(true);
        given(userRepository.existsByNickname("홍길동2")).willReturn(false);

        assertThat(generator.uniquify("홍길동")).isEqualTo("홍길동2");
    }

    @Test
    void uniquify_여러_후보가_중복이면_다음_순번_사용() {
        given(userRepository.existsByNickname("홍길동")).willReturn(true);
        given(userRepository.existsByNickname("홍길동2")).willReturn(true);
        given(userRepository.existsByNickname("홍길동3")).willReturn(false);

        assertThat(generator.uniquify("홍길동")).isEqualTo("홍길동3");
    }
}
