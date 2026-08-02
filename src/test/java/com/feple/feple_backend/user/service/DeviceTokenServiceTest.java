package com.feple.feple_backend.user.service;

import static com.feple.feple_backend.support.TestEntityFactory.user;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.user.entity.DevicePlatform;
import com.feple.feple_backend.user.entity.DeviceTokenRegistration;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserDeviceToken;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock UserDeviceTokenRepository tokenRepository;
    @Mock UserRepository userRepository;

    @InjectMocks DeviceTokenService service;

    @Test
    void register_기존토큰있으면_언어만_갱신() {
        UserDeviceToken existing = mock(UserDeviceToken.class);
        given(tokenRepository.findByUserIdAndToken(1L, "token-a")).willReturn(Optional.of(existing));

        service.register(1L, new DeviceTokenRegistration("token-a", "android", "ko"));

        verify(existing).updateLanguage("ko");
        verify(tokenRepository).deleteByTokenAndOtherUsers("token-a", 1L);
        verify(tokenRepository).deleteByUserIdAndPlatformExceptToken(1L, DevicePlatform.ANDROID, "token-a");
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void register_기존토큰없으면_신규_저장() {
        User author = user(1L);
        given(tokenRepository.findByUserIdAndToken(1L, "token-a")).willReturn(Optional.empty());
        given(userRepository.findById(1L)).willReturn(Optional.of(author));

        service.register(1L, new DeviceTokenRegistration("token-a", "android", "ko"));

        verify(tokenRepository).save(any(UserDeviceToken.class));
    }

    @Test
    void register_같은유저_같은플랫폼_이전토큰_정리() {
        User author = user(1L);
        given(tokenRepository.findByUserIdAndToken(1L, "new-token")).willReturn(Optional.empty());
        given(userRepository.findById(1L)).willReturn(Optional.of(author));

        service.register(1L, new DeviceTokenRegistration("new-token", "ios", "ko"));

        verify(tokenRepository).deleteByUserIdAndPlatformExceptToken(1L, DevicePlatform.IOS, "new-token");
    }

    @Test
    void register_알수없는_플랫폼이면_예외() {
        assertThatThrownBy(() -> service.register(1L, new DeviceTokenRegistration("token-a", "windows", "ko")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void register_사용자없으면_예외() {
        given(tokenRepository.findByUserIdAndToken(1L, "token-a")).willReturn(Optional.empty());
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(1L, new DeviceTokenRegistration("token-a", "android", "ko")))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void unregister_레포지토리에_위임() {
        service.unregister(1L, "token-a");

        verify(tokenRepository).deleteByUserIdAndToken(1L, "token-a");
    }

    @Test
    void deleteStaleTokens_빈목록이면_호출안함() {
        service.deleteStaleTokens(List.of());

        verify(tokenRepository, never()).deleteByTokenIn(any());
    }

    @Test
    void deleteStaleTokens_토큰있으면_삭제() {
        service.deleteStaleTokens(List.of("token-a", "token-b"));

        verify(tokenRepository).deleteByTokenIn(List.of("token-a", "token-b"));
    }
}
