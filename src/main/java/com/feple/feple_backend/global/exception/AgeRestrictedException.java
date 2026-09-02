package com.feple.feple_backend.global.exception;

/**
 * 나이 확인 결과 만 14세 미만으로 판정돼 서비스 이용이 차단된 경우.
 * 계정은 이미 소프트 삭제되었으며, 전역 예외 처리기가 403 + {@link ErrorCode#AGE_RESTRICTED}로 응답한다.
 */
public class AgeRestrictedException extends RuntimeException {

    public AgeRestrictedException(String message) {
        super(message);
    }
}
