package com.feple.feple_backend.admin.ocr;

import com.feple.feple_backend.global.exception.ErrorCode;
import com.feple.feple_backend.global.exception.ErrorResponse;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Gemini 호출 실패의 종류별 대응(예비 모델 전환 여부)과 관리자에게 보여줄 안내를 정의한다. */
enum GeminiFailure {

    OVERLOADED(true, HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.SERVICE_UNAVAILABLE,
            "Gemini가 일시적으로 혼잡합니다. 잠시 후 다시 시도해주세요."),
    TIMEOUT(true, HttpStatus.GATEWAY_TIMEOUT, ErrorCode.SERVICE_UNAVAILABLE,
            "Gemini 응답이 지연되고 있습니다. 잠시 후 다시 시도해주세요."),
    RATE_LIMITED(true, HttpStatus.TOO_MANY_REQUESTS, ErrorCode.RATE_LIMITED,
            "Gemini 사용 한도를 초과했습니다. 잠시 후 또는 내일 다시 시도해주세요."),
    FORBIDDEN(false, HttpStatus.BAD_GATEWAY, ErrorCode.SERVICE_UNAVAILABLE,
            "Gemini API 키 또는 권한 설정을 확인해주세요."),
    OTHER(false, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.SERVER_ERROR,
            "이미지 파싱에 실패했습니다. 다시 시도해주세요.");

    private final boolean fallbackAllowed;
    private final HttpStatus status;
    private final ErrorCode errorCode;
    private final String adminMessage;

    GeminiFailure(boolean fallbackAllowed, HttpStatus status, ErrorCode errorCode, String adminMessage) {
        this.fallbackAllowed = fallbackAllowed;
        this.status = status;
        this.errorCode = errorCode;
        this.adminMessage = adminMessage;
    }

    static GeminiFailure of(Exception e) {
        if (e instanceof WebClientResponseException.ServiceUnavailable) return OVERLOADED;
        if (e instanceof WebClientResponseException.TooManyRequests) return RATE_LIMITED;
        if (e instanceof WebClientResponseException.Forbidden) return FORBIDDEN;
        // Mono.block(Duration)은 제한 시간 초과를 TimeoutException을 감싼 IllegalStateException으로 던진다
        if (e.getCause() instanceof TimeoutException) return TIMEOUT;
        return OTHER;
    }

    boolean allowsFallback() {
        return fallbackAllowed;
    }

    // 프론트(AdminUtils.parseJsonOrThrow)가 error 필드를 안내 문구로 표시하므로 AdminErrorResponses와 같은 형식을 쓴다
    ResponseEntity<ErrorResponse> toResponse() {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, adminMessage, errorCode));
    }
}
