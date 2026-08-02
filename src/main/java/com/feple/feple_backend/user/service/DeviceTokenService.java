package com.feple.feple_backend.user.service;

import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.user.entity.DevicePlatform;
import com.feple.feple_backend.user.entity.DeviceTokenRegistration;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserDeviceToken;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final UserDeviceTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void register(Long userId, DeviceTokenRegistration registration) {
        String token = registration.token();
        DevicePlatform platform = DevicePlatform.from(registration.platform());
        // 같은 기기에서 계정 전환 시 동일 토큰이 다른 계정에 남아 있으면 제거
        tokenRepository.deleteByTokenAndOtherUsers(token, userId);
        // 재설치/토큰 로테이션으로 같은 유저·플랫폼에 이전 토큰이 쌓여 영구히 방치되는 것을 방지
        tokenRepository.deleteByUserIdAndPlatformExceptToken(userId, platform, token);

        tokenRepository.findByUserIdAndToken(userId, token).ifPresentOrElse(
            existing -> existing.updateLanguage(registration.language()),
            () -> {
                User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
                tokenRepository.save(UserDeviceToken.of(user, token, platform, registration.language()));
            }
        );
    }

    @Transactional
    public void unregister(Long userId, String token) {
        tokenRepository.deleteByUserIdAndToken(userId, token);
    }

    @Transactional
    public void deleteStaleTokens(List<String> tokens) {
        if (!tokens.isEmpty()) {
            tokenRepository.deleteByTokenIn(tokens);
        }
    }
}
