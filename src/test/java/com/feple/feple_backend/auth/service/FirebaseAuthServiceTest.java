package com.feple.feple_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.auth.firebase.FirebaseTokenVerifier;
import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import com.google.firebase.ErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;

@ExtendWith(MockitoExtension.class)
class FirebaseAuthServiceTest {

    @Mock OAuthUserRegistrationService registrationService;
    @Mock FirebaseTokenVerifier firebaseTokenVerifier;
    @Mock FirebaseToken firebaseToken;

    private FirebaseAuthService firebaseAuthService;

    @BeforeEach
    void setUp() {
        firebaseAuthService = new FirebaseAuthService(registrationService, firebaseTokenVerifier);
    }

    private User user() {
        return User.builder().id(1L).nickname("nick").oauthId("uid").provider(AuthProvider.FIREBASE).build();
    }

    @Test
    void 이메일_인증된_토큰이면_유저_등록후_반환() throws FirebaseAuthException {
        given(firebaseTokenVerifier.verify("id-token")).willReturn(firebaseToken);
        given(firebaseToken.getClaims()).willReturn(Map.of("email_verified", true));
        given(firebaseToken.getUid()).willReturn("uid-123");
        given(firebaseToken.getEmail()).willReturn("a@b.com");
        given(firebaseToken.getName()).willReturn("홍길동");
        User expected = user();
        given(registrationService.registerOrFind(new OAuthProfile(
                AuthProvider.FIREBASE, "uid-123", "a@b.com", "홍길동", "Useruid-123", null)))
                .willReturn(expected);

        User result = firebaseAuthService.authenticate("id-token").block();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void 이메일_미인증_토큰이면_예외() throws FirebaseAuthException {
        given(firebaseTokenVerifier.verify("id-token")).willReturn(firebaseToken);
        given(firebaseToken.getClaims()).willReturn(Map.of("email_verified", false));

        assertThatThrownBy(() -> firebaseAuthService.authenticate("id-token").block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일 인증");
    }

    @Test
    void emailVerified_클레임_없으면_예외() throws FirebaseAuthException {
        given(firebaseTokenVerifier.verify("id-token")).willReturn(firebaseToken);
        given(firebaseToken.getClaims()).willReturn(Map.of());

        assertThatThrownBy(() -> firebaseAuthService.authenticate("id-token").block())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 토큰_검증_실패시_일반화된_예외_메시지() throws FirebaseAuthException {
        given(firebaseTokenVerifier.verify("bad-token")).willThrow(new RuntimeException("invalid token"));

        assertThatThrownBy(() -> firebaseAuthService.authenticate("bad-token").block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("다시 로그인");
    }

    // 아래 세 가지는 "인증 실패(400)"로 뭉개면 안 된다 — 장애 중에 사용자가 재로그인을
    // 반복하게 되고, 5xx 모니터링에도 잡히지 않는다.
    @Test
    void DB_장애는_인증실패로_뭉개지_않고_그대로_전파() throws FirebaseAuthException {
        givenVerifiedToken("id-token");
        given(registrationService.registerOrFind(any()))
                .willThrow(new DataAccessResourceFailureException("connection pool exhausted"));

        assertThatThrownBy(() -> firebaseAuthService.authenticate("id-token").block())
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void Firebase_장애코드는_인증실패로_뭉개지_않고_그대로_전파() throws FirebaseAuthException {
        given(firebaseTokenVerifier.verify("id-token"))
                .willThrow(new FirebaseAuthException(
                        ErrorCode.UNAVAILABLE, "backend unavailable", null, null, null));

        // block()은 checked 예외를 ReactiveException으로 감싼다. 컨트롤러는 block()이
        // 아니라 onError로 받으므로 실제로는 원본이 그대로 전역 핸들러까지 간다.
        assertThatThrownBy(() -> firebaseAuthService.authenticate("id-token").block())
                .hasCauseInstanceOf(FirebaseAuthException.class);
    }

    @Test
    void 잘못된_토큰의_Firebase_예외는_인증실패로_변환() throws FirebaseAuthException {
        given(firebaseTokenVerifier.verify("bad-token"))
                .willThrow(new FirebaseAuthException(
                        ErrorCode.INVALID_ARGUMENT, "invalid id token", null, null, null));

        assertThatThrownBy(() -> firebaseAuthService.authenticate("bad-token").block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("다시 로그인");
    }

    @Test
    void 가입_재시도_소진_IllegalStateException은_그대로_전파() throws FirebaseAuthException {
        givenVerifiedToken("id-token");
        given(registrationService.registerOrFind(any()))
                .willThrow(new IllegalStateException("동시 가입 처리 중 예상치 못한 오류"));

        assertThatThrownBy(() -> firebaseAuthService.authenticate("id-token").block())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void displayName_없으면_fallback_닉네임_사용() throws FirebaseAuthException {
        given(firebaseTokenVerifier.verify("id-token")).willReturn(firebaseToken);
        given(firebaseToken.getClaims()).willReturn(Map.of("email_verified", true));
        given(firebaseToken.getUid()).willReturn("uid-45678900");
        given(firebaseToken.getEmail()).willReturn("a@b.com");
        given(firebaseToken.getName()).willReturn(null);
        given(registrationService.registerOrFind(any())).willReturn(user());

        firebaseAuthService.authenticate("id-token").block();

        ArgumentCaptor<OAuthProfile> captor = ArgumentCaptor.forClass(OAuthProfile.class);
        verify(registrationService).registerOrFind(captor.capture());
        assertThat(captor.getValue().rawNickname()).isEqualTo("Useruid-4567");
        assertThat(captor.getValue().fallbackNickname()).isEqualTo("Useruid-4567");
    }

    private void givenVerifiedToken(String idToken) throws FirebaseAuthException {
        given(firebaseTokenVerifier.verify(idToken)).willReturn(firebaseToken);
        given(firebaseToken.getClaims()).willReturn(Map.of("email_verified", true));
        given(firebaseToken.getUid()).willReturn("uid-12345678");
        given(firebaseToken.getEmail()).willReturn("a@b.com");
        given(firebaseToken.getName()).willReturn("tester");
    }
}
