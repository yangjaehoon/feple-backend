package com.feple.feple_backend.user.service;

import com.feple.feple_backend.user.entity.DevicePlatform;
import com.feple.feple_backend.user.entity.DeviceTokenRegistration;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserDeviceToken;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.feple.feple_backend.global.EntityLoader;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final UserDeviceTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void register(Long userId, DeviceTokenRegistration registration) {
        String token = registration.token();
        // 같은 기기에서 계정 전환 시 동일 토큰이 다른 계정에 남아 있으면 제거
        tokenRepository.deleteByTokenAndOtherUsers(token, userId);

        tokenRepository.findByUserIdAndToken(userId, token).ifPresentOrElse(
            existing -> existing.updateLanguage(registration.language()),
            () -> {
                User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
                tokenRepository.save(UserDeviceToken.of(user, token,
                        DevicePlatform.from(registration.platform()), registration.language()));
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
