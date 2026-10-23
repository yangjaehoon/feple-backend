package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.global.exception.AgeRestrictedException;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.global.exception.UserBannedException;

import com.feple.feple_backend.user.NicknameGenerator;
import com.feple.feple_backend.user.entity.AgePolicy;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.WithdrawalReason;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuthUserRegistrationService {

    private static final int MAX_NICKNAME_COLLISION_RETRIES = 3;

    private final UserRepository userRepository;
    private final NicknameGenerator nicknameGenerator;

    /**
     * OAuth 공급자(provider) + oauthId로 기존 유저를 조회하거나 신규 가입 처리.
     * 닉네임은 재시도마다 다시 생성해 매번 새로 유니크 여부를 확인한다 — 서로 다른
     * 두 신규 유저가 동시에 같은 닉네임 후보로 충돌해도(같은 provider+oauthId 경합이 아닌 경우)
     * 새 후보로 재시도한다.
     */
    public User registerOrFind(OAuthProfile profile) {
        Optional<User> existing = findExisting(profile);
        if (existing.isPresent()) {
            return requireLoginAllowed(existing.get());
        }
        return registerNew(profile, MAX_NICKNAME_COLLISION_RETRIES);
    }

    private Optional<User> findExisting(OAuthProfile profile) {
        return userRepository.findByProviderAndOauthId(profile.provider(), profile.oauthId());
    }

    /** 탈퇴·나이 제한·정지 계정이면 로그인을 거부하고, 아니면 그대로 반환한다. */
    private User requireLoginAllowed(User user) {
        if (user.isDeleted()) {
            if (user.getWithdrawalReason() == WithdrawalReason.AGE_RESTRICTED) {
                throw new AgeRestrictedException(AgePolicy.UNDER_MINIMUM_AGE_MESSAGE);
            }
            throw new InvalidRequestException("탈퇴 처리된 계정입니다. 동일한 계정으로 재가입할 수 없습니다.");
        }
        // 토큰을 내준 뒤 모든 인증 요청에서 403을 받게 하느니 로그인 단계에서 거부한다.
        if (user.isBanned()) {
            throw new UserBannedException();
        }
        return user;
    }

    private User registerNew(OAuthProfile profile, int attemptsLeft) {
        try {
            String nickname = nicknameGenerator.generateFrom(profile.rawNickname(), profile.fallbackNickname());
            return userRepository.save(profile.toUser(nickname));
        } catch (DataIntegrityViolationException e) {
            // 동일 유저의 동시 가입 경합(provider+oauthId unique)이면 이미 저장된 행을 반환
            Optional<User> existing = findExisting(profile);
            if (existing.isPresent()) return requireLoginAllowed(existing.get());
            // 그 외(서로 다른 신규 유저의 닉네임 후보 충돌 등)는 새 닉네임 후보로 재시도
            if (attemptsLeft > 1) {
                return registerNew(profile, attemptsLeft - 1);
            }
            // cause를 붙인다 — 닉네임·oauthId 외의 제약 위반(예: 컬럼 길이 초과)도 여기로
            // 떨어지는데, 원인 없이 올라가면 재시도 3회 후 로그에 단서가 남지 않는다.
            throw new IllegalStateException("동시 가입 처리 중 예상치 못한 오류", e);
        }
    }
}
