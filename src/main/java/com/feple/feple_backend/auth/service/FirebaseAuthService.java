package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.auth.firebase.FirebaseTokenVerifier;
import com.feple.feple_backend.global.exception.AgeRestrictedException;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import com.feple.feple_backend.user.NicknameGenerator;
import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import com.google.firebase.ErrorCode;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class FirebaseAuthService implements OAuthLoginService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthService.class);

    /** Firebase 쪽 장애로 판단하는 코드 — 사용자의 토큰 문제가 아니므로 5xx로 올려보낸다. */
    private static final Set<ErrorCode> UPSTREAM_OUTAGE_CODES = EnumSet.of(
            ErrorCode.UNAVAILABLE, ErrorCode.DEADLINE_EXCEEDED,
            ErrorCode.INTERNAL, ErrorCode.RESOURCE_EXHAUSTED);

    private final NicknameGenerator nicknameGenerator;
    private final OAuthUserRegistrationService registrationService;
    private final FirebaseTokenVerifier firebaseTokenVerifier;

    @Override
    public Mono<User> authenticate(String idToken) {
        // verifyIdToken은 블로킹 네트워크 호출 — 이벤트 루프 스레드 점유 방지를 위해 boundedElastic 사용
        return Mono.fromCallable(() -> authenticateSync(idToken))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private User authenticateSync(String idToken) throws FirebaseAuthException {
        try {
            FirebaseToken decoded = firebaseTokenVerifier.verify(idToken);
            Boolean emailVerified = (Boolean) decoded.getClaims().get("email_verified");
            if (emailVerified == null || !emailVerified) {
                throw new InvalidRequestException("이메일 인증이 완료되지 않았습니다.");
            }
            return registerOrFind(decoded.getUid(), decoded.getEmail(), decoded.getName());
        } catch (IllegalArgumentException | IllegalStateException | AgeRestrictedException e) {
            // 의도적으로 노출할 검증·나이 제한 메시지는 "인증 실패"로 뭉개지 않고 그대로 전파한다.
            // IllegalStateException(가입 재시도 소진 등 예상 불가 서버 오류)도 여기로 —
            // 400으로 뭉개면 5xx 모니터링에 안 잡히고 handleIllegalState의 log.error도 안 돈다.
            throw e;
        } catch (DataAccessException e) {
            // DB 장애·커넥션 풀 고갈을 400 "다시 로그인해주세요"로 바꾸면 장애 중에
            // 클라이언트가 재로그인을 반복한다 — 서버 오류로 그대로 올려보낸다.
            log.error("[Firebase Auth] 로그인 처리 중 DB 오류", e);
            throw e;
        } catch (FirebaseAuthException e) {
            // Firebase 자체 장애(무응답·타임아웃·내부 오류)는 잘못된 토큰과 구분한다.
            // 구분하지 않으면 장애 내내 사용자에게 "다시 로그인해주세요"를 띄워 재인증
            // 루프를 유발한다. 카카오 경로(KakaoApiClient)는 이미 4xx만 사용자 오류로
            // 변환하고 5xx는 그대로 올려보낸다.
            if (UPSTREAM_OUTAGE_CODES.contains(e.getErrorCode())) {
                log.error("[Firebase Auth] Firebase 장애로 토큰 검증 실패: {}", e.getErrorCode(), e);
                throw e;
            }
            log.warn("[Firebase Auth] idToken 검증 실패: {} - {}", e.getErrorCode(), e.getMessage());
            throw new InvalidRequestException("인증에 실패했습니다. 다시 로그인해주세요.");
        } catch (Exception e) {
            log.warn("[Firebase Auth] idToken 검증 실패: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw new InvalidRequestException("인증에 실패했습니다. 다시 로그인해주세요.");
        }
    }

    private User registerOrFind(String uid, String email, String displayName) {
        String fallback = "User" + uid.substring(0, Math.min(uid.length(), 8));
        String raw = (displayName != null && !displayName.isBlank()) ? displayName : fallback;
        return registrationService.registerOrFind(AuthProvider.FIREBASE, uid,
                () -> nicknameGenerator.generateFrom(raw, fallback),
                nickname -> User.builder()
                        .email(email)
                        .nickname(nickname)
                        .oauthId(uid)
                        .provider(AuthProvider.FIREBASE)
                        .build());
    }
}
