package com.feple.feple_backend.global.exception;

import java.util.NoSuchElementException;

/**
 * 요청한 리소스(엔티티)가 존재하지 않을 때 던지는, 클라이언트에게 그대로 노출해도 안전한 도메인 예외.
 *
 * <p>{@link NoSuchElementException}을 상속하므로 기존의 {@code catch (NoSuchElementException)} 블록과
 * {@code isInstanceOf(NoSuchElementException.class)} 테스트 단언은 그대로 동작한다
 * ({@link InvalidRequestException}이 {@link IllegalArgumentException}을 상속하는 것과 동일한 이유).
 *
 * <p>전역 예외 처리기는 이 타입의 메시지만 404 응답 본문에 노출하고, 순수
 * {@link NoSuchElementException}(JDK의 {@code Optional.get()}·{@code Iterator.next()} 등이 던진 것)의
 * 메시지는 내부 구현("No value present" 등)이 새지 않도록 일반 메시지로 대체한다.
 * 애플리케이션 코드에서 "찾을 수 없음"을 표현할 때는 항상 이 예외를 던질 것.
 */
public class ResourceNotFoundException extends NoSuchElementException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /** "{엔티티명}을(를) 찾을 수 없습니다: {id}" 형태의 표준 메시지로 생성한다. */
    public static ResourceNotFoundException of(String entityName, Object id) {
        return new ResourceNotFoundException(entityName + "을(를) 찾을 수 없습니다: " + id);
    }
}
