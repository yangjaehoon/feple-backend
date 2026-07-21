package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OAuthUserRegistrationServiceTest {

    @Mock UserRepository userRepository;

    private OAuthUserRegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new OAuthUserRegistrationService(userRepository);
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

        User result = registrationService.registerOrFind(AuthProvider.KAKAO, "oauth-1",
                () -> { throw new AssertionError("신규 유저가 있을 때는 nicknameSupplier가 호출되면 안 됨"); },
                nickname -> { throw new AssertionError("신규 유저가 있을 때는 builder가 호출되면 안 됨"); });

        assertThat(result).isEqualTo(existing);
        verify(userRepository, never()).save(any());
    }

    @Test
    void 탈퇴한_유저면_예외() {
        User deleted = deletedUser();
        given(userRepository.findByProviderAndOauthId(AuthProvider.KAKAO, "oauth-2"))
                .willReturn(Optional.of(deleted));

        assertThatThrownBy(() -> registrationService.registerOrFind(AuthProvider.KAKAO, "oauth-2",
                () -> { throw new AssertionError("호출되면 안 됨"); },
                nickname -> { throw new AssertionError("호출되면 안 됨"); }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("탈퇴");
    }

    @Test
    void 신규_유저면_저장후_반환() {
        given(userRepository.findByProviderAndOauthId(AuthProvider.KAKAO, "oauth-3"))
                .willReturn(Optional.empty());
        User newUser = User.builder().nickname("new").oauthId("oauth-3").provider(AuthProvider.KAKAO).build();
        given(userRepository.save(newUser)).willReturn(newUser);

        User result = registrationService.registerOrFind(AuthProvider.KAKAO, "oauth-3",
                () -> "new", nickname -> newUser);

        assertThat(result).isEqualTo(newUser);
    }

    @Test
    void 동시_가입_경합시_재조회로_기존_유저_반환() {
        User existing = activeUser();
        given(userRepository.findByProviderAndOauthId(AuthProvider.KAKAO, "oauth-4"))
                .willReturn(Optional.empty())
                .willReturn(Optional.of(existing));
        User newUser = User.builder().nickname("race").oauthId("oauth-4").provider(AuthProvider.KAKAO).build();
        given(userRepository.save(newUser)).willThrow(new DataIntegrityViolationException("dup"));

        User result = registrationService.registerOrFind(AuthProvider.KAKAO, "oauth-4",
                () -> "race", nickname -> newUser);

        assertThat(result).isEqualTo(existing);
    }

    @Test
    void 동시_가입_경합후_재조회도_실패하면_예외() {
        given(userRepository.findByProviderAndOauthId(AuthProvider.KAKAO, "oauth-5"))
                .willReturn(Optional.empty());
        User newUser = User.builder().nickname("race2").oauthId("oauth-5").provider(AuthProvider.KAKAO).build();
        given(userRepository.save(newUser)).willThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> registrationService.registerOrFind(AuthProvider.KAKAO, "oauth-5",
                () -> "race2", nickname -> newUser))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 서로_다른_신규유저의_닉네임_충돌시_새_닉네임으로_재시도후_성공() {
        // 서로 다른 두 사용자가 동시에 가입하며 자동 생성된 닉네임 후보가 우연히 같아 첫 저장은
        // 실패하지만, 동일 provider+oauthId 경합이 아니므로 재조회는 계속 비어있고,
        // nicknameSupplier를 다시 호출해 새 후보로 재시도한다.
        given(userRepository.findByProviderAndOauthId(AuthProvider.KAKAO, "oauth-6"))
                .willReturn(Optional.empty());
        User firstAttempt = User.builder().nickname("dup").oauthId("oauth-6").provider(AuthProvider.KAKAO).build();
        User secondAttempt = User.builder().nickname("dup2").oauthId("oauth-6").provider(AuthProvider.KAKAO).build();
        given(userRepository.save(firstAttempt)).willThrow(new DataIntegrityViolationException("dup nickname"));
        given(userRepository.save(secondAttempt)).willReturn(secondAttempt);

        Iterator<String> nicknames = List.of("dup", "dup2").iterator();
        User result = registrationService.registerOrFind(AuthProvider.KAKAO, "oauth-6",
                nicknames::next,
                nickname -> "dup".equals(nickname) ? firstAttempt : secondAttempt);

        assertThat(result).isEqualTo(secondAttempt);
        verify(userRepository).save(firstAttempt);
        verify(userRepository).save(secondAttempt);
    }
}
