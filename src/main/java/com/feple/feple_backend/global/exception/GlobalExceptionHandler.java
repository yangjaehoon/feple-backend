package com.feple.feple_backend.global.exception;

import com.feple.feple_backend.global.exception.TooManyRequestsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex) {
        log.info("Resource not found: {}", ex.getMessage());
        String msg = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : "리소스를 찾을 수 없습니다.";
        return body(HttpStatus.NOT_FOUND, msg, ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        FieldError first = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = (first != null) ? first.getDefaultMessage() : "입력값이 올바르지 않습니다.";
        String field = (first != null) ? first.getField() : null;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.withField(HttpStatus.BAD_REQUEST, message, ErrorCode.VALIDATION_FAILED, field));
    }

    // @RequestParam/@PathVariable에 붙은 @Min/@Max/@Size/@NotBlank 등 제약 위반 시 발생
    // (핸들러를 등록하지 않으면 catch-all Exception 핸들러로 떨어져 500이 반환됨)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        log.debug("Parameter validation failed: {}", ex.getMessage());
        return body(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다.", ErrorCode.VALIDATION_FAILED);
    }

    // 파라미터 타입 불일치 (예: Long 파라미터에 숫자가 아닌 값 전달)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.debug("Parameter type mismatch: {}", ex.getMessage());
        return body(HttpStatus.BAD_REQUEST, "요청 파라미터 형식이 올바르지 않습니다.", ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        log.debug("Missing required parameter: {}", ex.getMessage());
        return body(HttpStatus.BAD_REQUEST, "필수 파라미터가 누락되었습니다.", ErrorCode.INVALID_REQUEST);
    }

    // BadWordException extends IllegalArgumentException — Spring이 계층 깊이로 구체적인 핸들러를 먼저 선택
    @ExceptionHandler(BadWordException.class)
    public ResponseEntity<ErrorResponse> handleBadWord(BadWordException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.withField(HttpStatus.BAD_REQUEST, ex.getMessage(), ErrorCode.BAD_WORD, ex.getField()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.debug("Bad request: {}", ex.getMessage());
        return body(HttpStatus.BAD_REQUEST, ex.getMessage(), ErrorCode.ILLEGAL_ARGUMENT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return body(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", ErrorCode.ACCESS_DENIED);
    }

    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationRequired(AuthenticationRequiredException ex) {
        log.info("Authentication required: {}", ex.getMessage());
        return body(HttpStatus.UNAUTHORIZED, ex.getMessage(), ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return body(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), ErrorCode.RATE_LIMITED);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return body(HttpStatus.CONFLICT, ex.getMessage(), ErrorCode.CONFLICT);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        // 좋아요/팔로우 등 check-then-act 토글의 동시 요청 경합(unique 제약 위반)도 여기로 들어옴 — 정상 트래픽이므로 warn
        log.warn("DataIntegrityViolationException reached handler (동시 요청 경합 또는 서비스 레이어 ConflictException 변환 누락 가능): {}",
                ex.getMostSpecificCause().getClass().getSimpleName());
        log.debug("Data integrity violation detail", ex.getMostSpecificCause());
        return body(HttpStatus.CONFLICT, "이미 존재하는 데이터입니다.", ErrorCode.CONFLICT);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        log.debug("Malformed request body: {}", ex.getMessage());
        return body(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다.", ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다.", ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기가 허용 범위를 초과했습니다.", ErrorCode.FILE_TOO_LARGE);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        log.error("Illegal state: {}", ex.getMessage(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", ErrorCode.ILLEGAL_STATE);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException ex) {
        log.error("I/O error: {}", ex.getMessage(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "파일 처리 중 오류가 발생했습니다.", ErrorCode.IO_ERROR);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        return body(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다.", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", ErrorCode.SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> body(HttpStatus status, String message, ErrorCode code) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message, code));
    }
}
