package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Transactional
public class FirebaseAuthService implements OAuthLoginService {

    private final UserRepository userRepository;

    @Override
    public Mono<User> authenticate(String idToken) {
        return Mono.fromCallable(() -> authenticateSync(idToken))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private User authenticateSync(String idToken) {
        try {
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
            Boolean emailVerified = (Boolean) decoded.getClaims().get("email_verified");
            if (emailVerified == null || !emailVerified) {
                throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
            }
            return registerOrFind(decoded.getUid(), decoded.getEmail(), decoded.getName());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("인증에 실패했습니다. 다시 로그인해주세요.");
        }
    }

    private User registerOrFind(String uid, String email, String displayName) {
        return userRepository.findByProviderAndOauthId(AuthProvider.FIREBASE, uid)
                .map(user -> {
                    if (user.isDeleted()) {
                        throw new IllegalStateException("탈퇴 처리된 계정입니다. 동일한 계정으로 재가입할 수 없습니다.");
                    }
                    return user;
                }).orElseGet(() -> {
                    String raw = (displayName != null && !displayName.isBlank())
                            ? displayName
                            : "User" + uid.substring(0, Math.min(uid.length(), 8));
                    String sanitized = raw.trim().replaceAll("[^가-힣a-zA-Z0-9_]", "");
                    String base = sanitized.isBlank()
                            ? "User" + uid.substring(0, Math.min(uid.length(), 8))
                            : sanitized;
                    return userRepository.save(User.builder()
                            .email(email)
                            .nickname(uniqueNickname(base))
                            .oauthId(uid)
                            .provider(AuthProvider.FIREBASE)
                            .build());
                });
    }

    private String uniqueNickname(String base) {
        if (base.length() > 8) base = base.substring(0, 8);
        if (!userRepository.existsByNickname(base)) return base;
        for (int i = 2; i <= 999; i++) {
            String candidate = base.substring(0, Math.min(base.length(), 6)) + i;
            if (!userRepository.existsByNickname(candidate)) return candidate;
        }
        return base + System.currentTimeMillis() % 10000;
    }
}
