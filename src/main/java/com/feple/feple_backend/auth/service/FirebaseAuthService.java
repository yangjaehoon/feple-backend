package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.user.NicknameGenerator;
import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Transactional
public class FirebaseAuthService implements OAuthLoginService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthService.class);

    private final NicknameGenerator nicknameGenerator;
    private final OAuthUserRegistrationService registrationService;

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
            log.warn("[Firebase Auth] idToken 검증 실패: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw new IllegalArgumentException("인증에 실패했습니다. 다시 로그인해주세요.");
        }
    }

    private User registerOrFind(String uid, String email, String displayName) {
        String fallback = "User" + uid.substring(0, Math.min(uid.length(), 8));
        String raw = (displayName != null && !displayName.isBlank()) ? displayName : fallback;
        String nickname = nicknameGenerator.uniquify(nicknameGenerator.sanitize(raw, fallback));
        return registrationService.registerOrFind(AuthProvider.FIREBASE, uid, () ->
                User.builder()
                        .email(email)
                        .nickname(nickname)
                        .oauthId(uid)
                        .provider(AuthProvider.FIREBASE)
                        .build());
    }
}
