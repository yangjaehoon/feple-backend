package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class OAuthUserRegistrationService {

    private final UserRepository userRepository;

    /**
     * OAuth 공급자(provider) + oauthId로 기존 유저를 조회하거나 신규 가입 처리.
     * 동시 요청 경합(race condition) 시 DataIntegrityViolationException를 재시도 조회로 처리.
     */
    public User registerOrFind(AuthProvider provider, String oauthId, Supplier<User> userBuilder) {
        return userRepository.findByProviderAndOauthId(provider, oauthId)
                .map(user -> {
                    if (user.isDeleted()) {
                        throw new IllegalArgumentException("탈퇴 처리된 계정입니다. 동일한 계정으로 재가입할 수 없습니다.");
                    }
                    return user;
                }).orElseGet(() -> {
                    try {
                        return userRepository.save(userBuilder.get());
                    } catch (DataIntegrityViolationException e) {
                        return userRepository.findByProviderAndOauthId(provider, oauthId)
                                .orElseThrow(() -> new IllegalStateException("동시 가입 처리 중 예상치 못한 오류"));
                    }
                });
    }
}
