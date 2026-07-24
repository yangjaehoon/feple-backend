package com.feple.feple_backend.user;

import com.feple.feple_backend.artist.ArtistNameValidator;
import com.feple.feple_backend.badword.BadWordValidator;
import com.feple.feple_backend.nickname.NicknameRestrictionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 닉네임 콘텐츠 검증(금칙어/아티스트명/제한어) 순서를 한 곳에서 관리한다.
 * 금칙어 검증은 호출부마다 필요한 예외 타입이 달라(필드 태그 여부) 각자 호출하고,
 * 항상 동일하게 적용되는 아티스트명/제한어 검증만 여기서 묶는다.
 */
@Component
@RequiredArgsConstructor
public class NicknameContentValidator {

    private final BadWordValidator badWordValidator;
    private final ArtistNameValidator artistNameValidator;
    private final NicknameRestrictionValidator nicknameRestrictionValidator;

    public record Step(String failureCode, Consumer<String> validate) {}

    /** 실패 사유별 코드가 필요한 호출부용 (예: 닉네임 중복확인 API) */
    public List<Step> steps() {
        return List.of(
                new Step("BAD_WORD", badWordValidator::validate),
                new Step("ARTIST_NAME", artistNameValidator::validate),
                new Step("RESTRICTED", nicknameRestrictionValidator::validate)
        );
    }

    /** 실패 사유 구분이 필요 없는 호출부용 — 순서대로 검증, 첫 실패에서 예외 전파 */
    public void validate(String nickname) {
        badWordValidator.validate(nickname);
        artistNameValidator.validate(nickname);
        nicknameRestrictionValidator.validate(nickname);
    }

    /** 금칙어를 필드-태그 예외(BadWordException)로 별도 처리하는 호출부용 나머지 검증 */
    public void validateArtistAndRestriction(String nickname) {
        artistNameValidator.validate(nickname);
        nicknameRestrictionValidator.validate(nickname);
    }
}
