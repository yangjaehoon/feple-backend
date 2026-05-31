package com.feple.feple_backend.user.service;

import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserDeviceToken;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final UserDeviceTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void register(Long userId, String token, String platform) {
        // 같은 기기에서 계정 전환 시 동일 토큰이 다른 계정에 남아 있으면 제거
        tokenRepository.deleteByTokenAndOtherUsers(token, userId);

        if (tokenRepository.findByUserIdAndToken(userId, token).isPresent()) {
            return;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));
        tokenRepository.save(UserDeviceToken.of(user, token, platform));
    }

    @Transactional
    public void unregister(Long userId, String token) {
        tokenRepository.deleteByUserIdAndToken(userId, token);
    }
}
