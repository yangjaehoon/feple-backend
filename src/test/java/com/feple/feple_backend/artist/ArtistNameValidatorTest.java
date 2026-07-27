package com.feple.feple_backend.artist;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.feple.feple_backend.artist.repository.ArtistRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtistNameValidatorTest {

    @Mock ArtistRepository artistRepository;

    private ArtistNameValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ArtistNameValidator(artistRepository);
    }

    @Test
    void null_닉네임은_통과() {
        assertThatCode(() -> validator.validate(null)).doesNotThrowAnyException();
    }

    @Test
    void reload_전에는_모두_통과() {
        assertThatCode(() -> validator.validate("아무닉네임")).doesNotThrowAnyException();
    }

    @Test
    void 아티스트명을_포함하지_않으면_통과() {
        given(artistRepository.findAllKoreanNames()).willReturn(List.of("뉴진스"));
        given(artistRepository.findAllEnglishNames()).willReturn(List.of("NewJeans"));
        validator.reload();

        assertThatCode(() -> validator.validate("일반닉네임")).doesNotThrowAnyException();
    }

    @Test
    void 아티스트명을_포함하면_예외() {
        given(artistRepository.findAllKoreanNames()).willReturn(List.of("뉴진스"));
        given(artistRepository.findAllEnglishNames()).willReturn(List.of());
        validator.reload();

        assertThatThrownBy(() -> validator.validate("나는뉴진스팬"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("아티스트 이름");
    }

    @Test
    void 대소문자와_공백을_무시하고_매칭() {
        given(artistRepository.findAllKoreanNames()).willReturn(List.of());
        given(artistRepository.findAllEnglishNames()).willReturn(List.of("New Jeans"));
        validator.reload();

        assertThatThrownBy(() -> validator.validate("NEWJEANS"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 두글자_미만_이름은_필터링되어_매칭에서_제외() {
        given(artistRepository.findAllKoreanNames()).willReturn(List.of("A"));
        given(artistRepository.findAllEnglishNames()).willReturn(List.of());
        validator.reload();

        assertThatCode(() -> validator.validate("A")).doesNotThrowAnyException();
    }

    @Test
    void reload_재호출시_이전_목록은_사라지고_새_목록으로_대체() {
        given(artistRepository.findAllKoreanNames()).willReturn(List.of("뉴진스"));
        given(artistRepository.findAllEnglishNames()).willReturn(List.of());
        validator.reload();

        given(artistRepository.findAllKoreanNames()).willReturn(List.of("아이브"));
        validator.reload();

        assertThatCode(() -> validator.validate("나는뉴진스팬")).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validate("나는아이브팬")).isInstanceOf(IllegalArgumentException.class);
    }
}
