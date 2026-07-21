package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class OAuthUserRegistrationService {

    private static final int MAX_NICKNAME_COLLISION_RETRIES = 3;

    private final UserRepository userRepository;

    /**
     * OAuth 공급자(provider) + oauthId로 기존 유저를 조회하거나 신규 가입 처리.
     * nicknameSupplier는 재시도마다 다시 호출되어 매번 새로 유니크 여부를 확인한다 — 서로 다른
     * 두 신규 유저가 동시에 같은 닉네임 후보로 충돌해도(같은 provider+oauthId 경합이 아닌 경우)
     * 닉네임을 다시 생성해 재시도한다.
     */
    public User registerOrFind(AuthProvider provider, String oauthId,
                                Supplier<String> nicknameSupplier, Function<String, User> userBuilder) {
        Optional<User> existing = userRepository.findByProviderAndOauthId(provider, oauthId);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.isDeleted()) {
                throw new IllegalArgumentException("탈퇴 처리된 계정입니다. 동일한 계정으로 재가입할 수 없습니다.");
            }
            return user;
        }
        return registerNew(provider, oauthId, nicknameSupplier, userBuilder, MAX_NICKNAME_COLLISION_RETRIES);
    }

    private User registerNew(AuthProvider provider, String oauthId,
                              Supplier<String> nicknameSupplier, Function<String, User> userBuilder,
                              int attemptsLeft) {
        try {
            return userRepository.save(userBuilder.apply(nicknameSupplier.get()));
        } catch (DataIntegrityViolationException e) {
            // 동일 유저의 동시 가입 경합(provider+oauthId unique)이면 이미 저장된 행을 반환
            Optional<User> existing = userRepository.findByProviderAndOauthId(provider, oauthId);
            if (existing.isPresent()) return existing.get();
            // 그 외(서로 다른 신규 유저의 닉네임 후보 충돌 등)는 새 닉네임 후보로 재시도
            if (attemptsLeft > 1) {
                return registerNew(provider, oauthId, nicknameSupplier, userBuilder, attemptsLeft - 1);
            }
            throw new IllegalStateException("동시 가입 처리 중 예상치 못한 오류");
        }
    }
}
