package com.feple.feple_backend.auth.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Component;

/**
 * FirebaseAuth 정적 호출을 감싸는 얇은 래퍼.
 * KakaoApiClient와 동일하게 외부 SDK 호출을 주입 가능한 협력 객체로 분리해
 * FirebaseAuthService를 static mocking 없이 단위 테스트할 수 있게 한다.
 */
@Component
public class FirebaseTokenVerifier {

    public FirebaseToken verify(String idToken) throws FirebaseAuthException {
        return FirebaseAuth.getInstance().verifyIdToken(idToken);
    }
}
