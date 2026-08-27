package com.feple.feple_backend.global.exception;

/**
 * 클라이언트에게 그대로 노출해도 안전한, 의도적으로 작성한 검증·비즈니스 규칙 위반 메시지를 담는 예외.
 *
 * <p>{@link IllegalArgumentException}을 상속하므로 기존의 {@code catch (IllegalArgumentException)}
 * 블록과 {@code isInstanceOf(IllegalArgumentException.class)} 테스트 단언은 그대로 동작한다.
 * 전역 예외 처리기는 이 타입의 메시지만 응답 본문에 노출하고, 순수
 * {@code IllegalArgumentException}(JDK·라이브러리가 던진 것)의 메시지는 내부 구현이 새지 않도록
 * 일반 메시지로 대체한다. 사용자에게 보여줄 검증 실패는 항상 이 예외를 던질 것.
 */
public class InvalidRequestException extends IllegalArgumentException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
