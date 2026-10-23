package com.feple.feple_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.global.exception.AgeRestrictedException;
import com.feple.feple_backend.global.exception.UserBannedException;
import com.feple.feple_backend.user.NicknameGenerator;
import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.WithdrawalReason;
import com.feple.feple_backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class OAuthUserRegistrationServiceTest {

    @Mock UserRepository userRepository;
    @Mock NicknameGenerator nicknameGenerator;

    private OAuthUserRegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new OAuthUserRegistrationService(userRepository, nicknameGenerator);
    }

    private static OAuthProfile profile(String oauthId) {
        return new OAuthProfile(AuthProvider.KAKAO, oauthId, "a@b.com", "raw", "fallback", "http://img");
    }

    private User activeUser() {
        return User.builder().id(1L).nickname("nick").oauthId("oauth-1").provider(AuthProvider.KAKAO).build();
    }

    private User deletedUser() {
        return User.builder().id(2L).nickname("nick2").oauthId("oauth-2")
                .provider(AuthProvider.KAKAO).deletedAt(LocalDateTime.now()).build();
    }

    @Test
    void 기존_유저가_있으면_신규_생성없이_반환() {
        User existing = activeUser();
        given(userRepository.findByProviderAndOauthId(AuthProvider.KAKAO, "oauth-1"))
                .willReturn(Optional.of(existing));

        User result = registrationService.registerOrFind(profile("oauth-1"));

        assertThat(result).isEqualTo(existing);
        verify(userRepository, never()).save(any());
        verify(nicknameGenerator, never()).generateFrom(any(), any());
    }

    @Test
    void 탈퇴한_유저면_예외() {
        User deleted = deletedUser();
        given(userRepository.findByProviderAndOauthId(AuthProvider.KAKAO, "oauth-2"))
                .willReturn(Optional.of(deleted));

        assertThatThrownBy(() -> registrationService.registerOrFind(profile("oauth-2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("탈퇴");
    }

    @Test
    void 나이제한으로_삭제된_계정이면_AgeRestrictedException() {
        User ageRestricted = User.builder().id(3L).nickname("nick3").oauthId("oauth-age")
                .provider(AuthProvider.KAKAO).deletedAt(LocalDateTime.now())
                .withdrawalReason(WithdrawalReason.AGE_RESTRICTED).build();
        given(userRepository.findByProviderAndOauthId(AuthProvider.KAKAO, "oauth-age"))
                .willReturn(Optional.of(ageRestricted));

        assertThatThrownBy(() -> registrationService.registerOrFind(profile("oauth-age")))
                .isInstanceOf(AgeRestrictedException.class)
                .hasMessageContaining("14세");
    }

    @Test
    void 정지된_유저면_UserBannedException() {
        User banned = User.builder().id(4L).nickname("nick4").oauthId("oauth-banned")
                .provider(AuthProvider.KAKAO).bannedUntil(LocalDateTime.now().plusDays(3)).build();
        given(userRepository.findByProviderAndOauthId(AuthProvider.KAKAO, "oauth-banned"))
                .willReturn(Optional.of(banned));

        assertThatThrownBy(() -> registrationService.registerOrFind(profile("oauth-banned")))
                .isInstanceOf(UserBannedException.class);
    }

    @Test
    void 신규_유저면_프로필과_생성된_닉네임으로_저장후_반환() {
        given(userRepository.findByProviderAndOauthId(AuthProvider.KAKAO, "oauth-3"))
                .willReturn(Optional.empty());
        given(nicknameGenerator.generateFrom("raw", "fallback")).willReturn("new");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        User result = registrationService.registerOrFind(profile("oauth-3"));

        assertThat(result.getProvider()).isEqualTo(AuthProvider.KAKAO);
        assertThat(result.getOauthId()).isEqualTo("oauth-3");
        assertThat(result.getEmail()).isEqualTo("a@b.com");
        assertThat(result.getNickname()).isEqualTo("new");
        assertThat(result.getProfileImageUrl()).isEqualTo("http://img");
    }

    @Test
    void 동시_가입_경합시_재조회로_기존_유저_반환() {
        User existing = activeUser();
        given(userRepository.findByProviderAndOauthId(AuthProvider.KAKAO, "oauth-4"))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(existing));
        given(nicknameGenerator.generateFrom(any(), any())).willReturn("race");
        given(userRepository.save(any(User.class))).willThrow(new DataIntegrityViolationException("dup"));

        User result = registrationService.registerOrFind(profile("oauth-4"));

        assertThat(result).isEqualTo(existing);
    }

    @Test
    void 동시_가입_경합후_재조회도_실패하면_예외() {
        given(userRepository.findByProviderAndOauthId(AuthProvider.KAKAO, "oauth-5"))
                .willReturn(Optional.empty());
        given(nicknameGenerator.generateFrom(any(), any())).willReturn("race2");
        given(userRepository.save(any(User.class))).willThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> registrationService.registerOrFind(profile("oauth-5")))
                .isInstanceOf(IllegalStateException.class)
                // 닉네임·oauthId 외의 제약 위반도 여기로 떨어지므로 원인을 잃으면 안 된다
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 서로_다른_신규유저의_닉네임_충돌시_새_닉네임으로_재시도후_성공() {
        // 서로 다른 두 사용자가 동시에 가입하며 자동 생성된 닉네임 후보가 우연히 같아 첫 저장은
        // 실패하지만, 동일 provider+oauthId 경합이 아니므로 재조회는 계속 비어있고,
        // 닉네임을 다시 생성해 새 후보로 재시도한다.
        given(userRepository.findByProviderAndOauthId(AuthProvider.KAKAO, "oauth-6"))
                .willReturn(Optional.empty());
        given(nicknameGenerator.generateFrom("raw", "fallback")).willReturn("dup", "dup2");
        given(userRepository.save(argThat(u -> u != null && "dup".equals(u.getNickname()))))
                .willThrow(new DataIntegrityViolationException("dup nickname"));
        given(userRepository.save(argThat(u -> u != null && "dup2".equals(u.getNickname()))))
                .willAnswer(inv -> inv.getArgument(0));

        User result = registrationService.registerOrFind(profile("oauth-6"));

        assertThat(result.getNickname()).isEqualTo("dup2");
    }
}
