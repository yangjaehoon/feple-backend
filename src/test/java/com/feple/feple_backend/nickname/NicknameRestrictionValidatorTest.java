package com.feple.feple_backend.nickname;

import com.feple.feple_backend.nickname.event.NicknameRestrictionChangedEvent;
import com.feple.feple_backend.nickname.repository.NicknameRestrictionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NicknameRestrictionValidatorTest {

    @Mock NicknameRestrictionRepository repository;

    private NicknameRestrictionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new NicknameRestrictionValidator(repository);
    }

    @Test
    void null_닉네임은_통과() {
        assertThatCode(() -> validator.validate(null)).doesNotThrowAnyException();
    }

    @Test
    void 제한어가_로드되지_않았으면_모두_통과() {
        assertThatCode(() -> validator.validate("아무닉네임")).doesNotThrowAnyException();
    }

    @Test
    void 제한어_포함시_예외() {
        given(repository.findAllWords()).willReturn(List.of("금지어"));
        validator.reloadRestrictions();

        assertThatThrownBy(() -> validator.validate("금지어닉네임"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("사용할 수 없는 단어");
    }

    @Test
    void 제한어_미포함시_통과() {
        given(repository.findAllWords()).willReturn(List.of("금지어"));
        validator.reloadRestrictions();

        assertThatCode(() -> validator.validate("정상닉네임")).doesNotThrowAnyException();
    }

    @Test
    void 변경_이벤트_수신시_제한어_다시_로드() {
        given(repository.findAllWords()).willReturn(List.of());
        validator.reloadRestrictions();

        validator.handleChange(new NicknameRestrictionChangedEvent());

        verify(repository, times(2)).findAllWords();
    }
}
