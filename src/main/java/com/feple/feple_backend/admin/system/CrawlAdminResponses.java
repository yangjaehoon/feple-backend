package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.global.exception.ErrorCode;
import com.feple.feple_backend.global.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** 크롤/OCR 관리자 컨트롤러들(WebScrapeAdminController, TimetableOcrAdminController,
 * ArtistLineupOcrAdminController)이 공유하는 에러 응답 헬퍼. */
final class CrawlAdminResponses {

    private CrawlAdminResponses() {}

    static ResponseEntity<ErrorResponse> badRequest(String error) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, error, ErrorCode.ILLEGAL_ARGUMENT));
    }

    static ResponseEntity<ErrorResponse> serverError(String error) {
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, error, ErrorCode.SERVER_ERROR));
    }

    static ResponseEntity<ErrorResponse> geminiNotConfigured() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE,
                        "Gemini API 키가 설정되지 않았습니다. application-local.yaml에 app.gemini.api-key를 설정하세요.",
                        ErrorCode.SERVICE_UNAVAILABLE));
    }
}
