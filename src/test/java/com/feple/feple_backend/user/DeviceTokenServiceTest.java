package com.feple.feple_backend.user;

import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserDeviceToken;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.user.service.DeviceTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock UserDeviceTokenRepository tokenRepository;
    @Mock UserRepository userRepository;

    @InjectMocks DeviceTokenService deviceTokenService;

    @Test
    void register_이미_등록된_토큰은_저장_안_함() {
        given(tokenRepository.findByUserIdAndToken(1L, "token"))
                .willReturn(Optional.of(mock(UserDeviceToken.class)));

        deviceTokenService.register(1L, "token", "android", "ko");

        then(tokenRepository).should().deleteByTokenAndOtherUsers("token", 1L);
        then(tokenRepository).should(never()).save(any());
    }

    @Test
    void register_신규_토큰_등록() {
        given(tokenRepository.findByUserIdAndToken(1L, "token")).willReturn(Optional.empty());
        given(userRepository.findById(1L)).willReturn(Optional.of(mock(User.class)));

        deviceTokenService.register(1L, "token", "android", "ko");

        then(tokenRepository).should().save(any(UserDeviceToken.class));
    }

    @Test
    void register_다른_사용자_토큰_먼저_삭제() {
        given(tokenRepository.findByUserIdAndToken(1L, "token")).willReturn(Optional.empty());
        given(userRepository.findById(1L)).willReturn(Optional.of(mock(User.class)));

        deviceTokenService.register(1L, "token", "android", "ko");

        then(tokenRepository).should().deleteByTokenAndOtherUsers("token", 1L);
    }

    @Test
    void deleteStaleTokens_빈_리스트_삭제_안_함() {
        deviceTokenService.deleteStaleTokens(List.of());

        then(tokenRepository).should(never()).deleteByTokenIn(any());
    }

    @Test
    void deleteStaleTokens_토큰_삭제_호출() {
        deviceTokenService.deleteStaleTokens(List.of("t1", "t2"));

        then(tokenRepository).should().deleteByTokenIn(List.of("t1", "t2"));
    }
}
