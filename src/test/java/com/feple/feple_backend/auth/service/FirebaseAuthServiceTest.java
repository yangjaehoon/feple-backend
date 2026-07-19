package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.user.NicknameGenerator;
import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FirebaseAuthServiceTest {

    @Mock NicknameGenerator nicknameGenerator;
    @Mock OAuthUserRegistrationService registrationService;
    @Mock FirebaseAuth firebaseAuth;
    @Mock FirebaseToken firebaseToken;

    private FirebaseAuthService firebaseAuthService;

    @BeforeEach
    void setUp() {
        firebaseAuthService = new FirebaseAuthService(nicknameGenerator, registrationService);
    }

    private User user() {
        return User.builder().id(1L).nickname("nick").oauthId("uid").provider(AuthProvider.FIREBASE).build();
    }

    @Test
    void 이메일_인증된_토큰이면_유저_등록후_반환() throws FirebaseAuthException {
        given(firebaseToken.getClaims()).willReturn(Map.of("email_verified", true));
        given(firebaseToken.getUid()).willReturn("uid-123");
        given(firebaseToken.getEmail()).willReturn("a@b.com");
        given(firebaseToken.getName()).willReturn("홍길동");
        given(nicknameGenerator.sanitize("홍길동", "Useruid-123")).willReturn("홍길동");
        given(nicknameGenerator.uniquify("홍길동")).willReturn("홍길동");
        User expected = user();
        given(registrationService.registerOrFind(eq(AuthProvider.FIREBASE), eq("uid-123"), any()))
                .willReturn(expected);

        try (MockedStatic<FirebaseAuth> mocked = mockStatic(FirebaseAuth.class)) {
            mocked.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            given(firebaseAuth.verifyIdToken("id-token")).willReturn(firebaseToken);

            User result = firebaseAuthService.authenticate("id-token").block();

            assertThat(result).isEqualTo(expected);
        }
    }

    @Test
    void 이메일_미인증_토큰이면_예외() throws FirebaseAuthException {
        given(firebaseToken.getClaims()).willReturn(Map.of("email_verified", false));

        try (MockedStatic<FirebaseAuth> mocked = mockStatic(FirebaseAuth.class)) {
            mocked.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            given(firebaseAuth.verifyIdToken("id-token")).willReturn(firebaseToken);

            assertThatThrownBy(() -> firebaseAuthService.authenticate("id-token").block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이메일 인증");
        }
    }

    @Test
    void emailVerified_클레임_없으면_예외() throws FirebaseAuthException {
        given(firebaseToken.getClaims()).willReturn(Map.of());

        try (MockedStatic<FirebaseAuth> mocked = mockStatic(FirebaseAuth.class)) {
            mocked.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            given(firebaseAuth.verifyIdToken("id-token")).willReturn(firebaseToken);

            assertThatThrownBy(() -> firebaseAuthService.authenticate("id-token").block())
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void 토큰_검증_실패시_일반화된_예외_메시지() throws FirebaseAuthException {
        try (MockedStatic<FirebaseAuth> mocked = mockStatic(FirebaseAuth.class)) {
            mocked.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            given(firebaseAuth.verifyIdToken("bad-token")).willThrow(new RuntimeException("invalid token"));

            assertThatThrownBy(() -> firebaseAuthService.authenticate("bad-token").block())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("다시 로그인");
        }
    }

    @Test
    void displayName_없으면_fallback_닉네임_사용() throws FirebaseAuthException {
        given(firebaseToken.getClaims()).willReturn(Map.of("email_verified", true));
        given(firebaseToken.getUid()).willReturn("uid-45678900");
        given(firebaseToken.getEmail()).willReturn("a@b.com");
        given(firebaseToken.getName()).willReturn(null);
        given(nicknameGenerator.sanitize("Useruid-4567", "Useruid-4567")).willReturn("Useruid-4567");
        given(nicknameGenerator.uniquify("Useruid-4567")).willReturn("Useruid-4567");
        given(registrationService.registerOrFind(any(), any(), any())).willReturn(user());

        try (MockedStatic<FirebaseAuth> mocked = mockStatic(FirebaseAuth.class)) {
            mocked.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            given(firebaseAuth.verifyIdToken("id-token")).willReturn(firebaseToken);

            firebaseAuthService.authenticate("id-token").block();

            verify(nicknameGenerator).sanitize("Useruid-4567", "Useruid-4567");
        }
    }
}
