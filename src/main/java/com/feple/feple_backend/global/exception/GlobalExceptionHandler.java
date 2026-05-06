package com.feple.feple_backend.global.exception;

import com.feple.feple_backend.auth.ratelimit.TooManyRequestsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 리소스를 찾지 못했을 때 — 내부 ID는 클라이언트에 노출하지 않고 로그에만 기록
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException ex) {
        log.info("Resource not found: {}", ex.getMessage());
        return errorBody(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다.");
    }

    // 잘못된 요청 값(DTO @Valid 실패)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return errorBody(HttpStatus.BAD_REQUEST, message);
    }

    // 잘못된 인자 (파일 형식, 크기, 비즈니스 유효성 등)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.debug("Bad request: {}", ex.getMessage());
        return errorBody(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 권한 없음 (IDOR, 타인 리소스 접근 시도) — 보안 모니터링을 위해 WARN 로깅
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return errorBody(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
    }

    // 로그인 필요
    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationRequired(AuthenticationRequiredException ex) {
        log.info("Authentication required: {}", ex.getMessage());
        return errorBody(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    // 로그인 시도 초과 (Rate Limit) — 공격 모니터링을 위해 WARN 로깅
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyRequests(TooManyRequestsException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return errorBody(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    // 중복 리소스
    @ExceptionHandler(DuplicateArtistFestivalException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateArtistFestivalException ex) {
        return errorBody(HttpStatus.CONFLICT, ex.getMessage());
    }

    // 잘못된 JSON 형식 — Spring 기본 에러 응답 대신 일관된 형식 반환
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        log.debug("Malformed request body: {}", ex.getMessage());
        return errorBody(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다.");
    }

    // 지원하지 않는 HTTP 메서드
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return errorBody(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다.");
    }

    // 파일 크기 초과 (멀티파트 레이어에서 발생)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return errorBody(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기가 허용 범위를 초과했습니다.");
    }

    // 내부 상태 오류 (예: JCA 알고리즘 미지원) — 운영 환경에서는 발생하면 안 되는 예외
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.error("Illegal state: {}", ex.getMessage(), ex);
        return errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }

    // 파일 I/O 오류 — 경로 등 시스템 정보를 클라이언트에 노출하지 않고 로그에만 기록
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> handleIOException(IOException ex) {
        log.error("I/O error: {}", ex.getMessage(), ex);
        return errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "파일 처리 중 오류가 발생했습니다.");
    }

    // 정적 리소스 없음 (소스맵 등 브라우저 자동 요청) — 로깅 불필요
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        return errorBody(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다.");
    }

    // 그 외 모든 예외 — 내부 메시지 노출 금지, 서버 로그에만 기록
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return errorBody(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
    }

    private ResponseEntity<Map<String, Object>> errorBody(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
