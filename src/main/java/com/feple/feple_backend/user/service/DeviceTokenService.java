package com.feple.feple_backend.user.service;

import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserDeviceToken;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final UserDeviceTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void register(Long userId, String token, String platform) {
        // 이미 등록된 토큰이면 업데이트 타임스탬프만 갱신 (upsert)
        if (tokenRepository.findByUserIdAndToken(userId, token).isPresent()) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        tokenRepository.save(UserDeviceToken.of(user, token, platform));
    }

    @Transactional
    public void unregister(Long userId, String token) {
        tokenRepository.deleteByUserIdAndToken(userId, token);
    }
}
