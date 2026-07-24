package com.feple.feple_backend.user;

import com.feple.feple_backend.artist.ArtistNameValidator;
import com.feple.feple_backend.badword.BadWordValidator;
import com.feple.feple_backend.nickname.NicknameRestrictionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NicknameContentValidatorTest {

    @Mock BadWordValidator badWordValidator;
    @Mock ArtistNameValidator artistNameValidator;
    @Mock NicknameRestrictionValidator nicknameRestrictionValidator;

    private NicknameContentValidator validator;

    @BeforeEach
    void setUp() {
        validator = new NicknameContentValidator(badWordValidator, artistNameValidator, nicknameRestrictionValidator);
    }

    @Test
    void steps는_순서대로_3단계_구성() {
        List<NicknameContentValidator.Step> steps = validator.steps();

        assertThat(steps).extracting(NicknameContentValidator.Step::failureCode)
                .containsExactly("BAD_WORD", "ARTIST_NAME", "RESTRICTED");
    }

    @Test
    void steps의_각_단계는_해당_validator에_위임() {
        List<NicknameContentValidator.Step> steps = validator.steps();

        steps.get(0).validate().accept("닉네임");
        steps.get(1).validate().accept("닉네임");
        steps.get(2).validate().accept("닉네임");

        verify(badWordValidator).validate("닉네임");
        verify(artistNameValidator).validate("닉네임");
        verify(nicknameRestrictionValidator).validate("닉네임");
    }

    @Test
    void validate는_금칙어_아티스트명_제한어_순서로_검증() {
        validator.validate("닉네임");

        InOrder inOrder = Mockito.inOrder(badWordValidator, artistNameValidator, nicknameRestrictionValidator);
        inOrder.verify(badWordValidator).validate("닉네임");
        inOrder.verify(artistNameValidator).validate("닉네임");
        inOrder.verify(nicknameRestrictionValidator).validate("닉네임");
    }

    @Test
    void validate는_첫_실패에서_예외_전파하고_이후단계_생략() {
        willThrow(new IllegalArgumentException("금칙어가 포함되어 있습니다."))
                .given(badWordValidator).validate("금칙어닉네임");

        assertThatThrownBy(() -> validator.validate("금칙어닉네임"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(artistNameValidator, nicknameRestrictionValidator);
    }

    @Test
    void validateArtistAndRestriction은_금칙어_검증_생략() {
        validator.validateArtistAndRestriction("닉네임");

        verifyNoInteractions(badWordValidator);
        verify(artistNameValidator).validate("닉네임");
        verify(nicknameRestrictionValidator).validate("닉네임");
    }
}
