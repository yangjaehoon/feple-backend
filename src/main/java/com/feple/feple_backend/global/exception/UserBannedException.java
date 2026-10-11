package com.feple.feple_backend.global.exception;

/**
 * 정지된 계정이 로그인·토큰 갱신을 시도한 경우.
 * 전역 예외 처리기가 403 + {@link ErrorCode#USER_BANNED}로 응답한다 — JwtAuthenticationFilter가
 * 보호된 엔드포인트에서 내려주는 정지 응답과 같은 코드라, 클라이언트는 한 가지 판정으로 처리한다.
 */
public class UserBannedException extends RuntimeException {

    public UserBannedException() {
        super("계정이 정지되었습니다.");
    }
}
