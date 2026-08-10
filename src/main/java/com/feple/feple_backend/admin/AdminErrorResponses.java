package com.feple.feple_backend.admin;

import com.feple.feple_backend.global.exception.ErrorCode;
import com.feple.feple_backend.global.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/** 크롤/OCR 관리자 컨트롤러들(WebScrapeAdminController, TimetableOcrAdminController,
 * ArtistLineupOcrAdminController)이 서로 다른 하위 패키지(admin.scraper, admin.ocr)에
 * 있어 공개 접근이 필요한 공유 에러 응답 헬퍼. */
public final class AdminErrorResponses {

    private AdminErrorResponses() {}

    public static ResponseEntity<ErrorResponse> badRequest(String error) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(HttpStatus.BAD_REQUEST, error, ErrorCode.ILLEGAL_ARGUMENT));
    }

    public static ResponseEntity<ErrorResponse> serverError(String error) {
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR, error, ErrorCode.SERVER_ERROR));
    }

    // OCR 컨트롤러들이 업로드받은 파일을 그대로 base64 인코딩해 Gemini API에 전달하므로,
    // content-type이 이미지가 아닌 파일(PDF·실행파일 등)이 크기 제한만 통과하면 그대로
    // 전송되는 것을 막기 위한 화이트리스트 검증.
    public static boolean isNotImage(MultipartFile image) {
        String contentType = image.getContentType();
        return contentType == null || !contentType.startsWith("image/");
    }

    public static ResponseEntity<ErrorResponse> geminiNotConfigured() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of(HttpStatus.SERVICE_UNAVAILABLE,
                        "Gemini API 키가 설정되지 않았습니다. application-local.yaml에 app.gemini.api-key를 설정하세요.",
                        ErrorCode.SERVICE_UNAVAILABLE));
    }
}
