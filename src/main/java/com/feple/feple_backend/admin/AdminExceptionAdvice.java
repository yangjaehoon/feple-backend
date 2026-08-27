package com.feple.feple_backend.admin;

import com.feple.feple_backend.global.exception.ErrorCode;
import com.feple.feple_backend.global.exception.ErrorResponse;
import com.feple.feple_backend.global.exception.InvalidRequestException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

/**
 * 관리자 페이지(<code>/admin/**</code>) 전용 예외 처리기.
 *
 * <p>전역 {@code GlobalExceptionHandler}는 {@code @RestControllerAdvice}라 모든 예외를 JSON으로 응답한다.
 * 관리자 화면은 브라우저로 여는 Thymeleaf 페이지이므로, 잘못된 쿼리 파라미터(날짜 바인딩 실패 등)나
 * 예상 못한 런타임 예외가 컨트롤러 밖으로 새면 사용자에게 raw JSON이 노출됐다. 이 advice는 관리자
 * 컨트롤러에서 올라온 예외를 가로채 HTML 에러 페이지로 렌더링한다.
 *
 * <p>단, {@code @ResponseBody} 메서드(OCR·크롤링·푸시 등 관리자 JSON API)는 프런트 스크립트가
 * JSON 응답을 기대하므로 전역 핸들러와 동일한 {@link ErrorResponse} 형태로 응답한다.
 *
 * <p>{@code @Order(HIGHEST_PRECEDENCE)}: 같은 예외에 대해 전역 핸들러보다 이 advice가 먼저
 * 선택되도록 우선순위를 높인다. 그 결과 관리자 컨트롤러에서 발생한 예외는 전역
 * {@code GlobalExceptionHandler}의 타입별 핸들러(409/429 등)를 거치지 않고 여기서 끝난다 —
 * 관리자 {@code @ResponseBody} 엔드포인트가 특정 상태 코드를 내려야 한다면 컨트롤러에서
 * 직접 {@code ResponseEntity}로 처리할 것(예상 밖 예외는 전부 500으로 매핑된다).
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(basePackages = "com.feple.feple_backend.admin")
public class AdminExceptionAdvice {

    static final String ERROR_VIEW = "admin/error";

    // 사용자가 응답 수신 도중 연결을 끊은 흔한 상황 — 서버 오류가 아니므로 조용히 넘긴다.
    @ExceptionHandler(ClientAbortException.class)
    public void handleClientAbort(ClientAbortException ex) {
        log.debug("관리자 응답 전송 중 클라이언트 연결 종료: {}", ex.getMessage());
    }

    // 메서드 레벨 @PreAuthorize 등에서 올라온 접근 거부는 catch-all(500)로 흘려보내지 않고
    // 인터셉터의 거부와 동일하게 접근 거부 화면으로 보낸다.
    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.debug("관리자 페이지 접근 거부: {} {}", request.getMethod(), request.getRequestURI());
        return "redirect:/admin/access-denied";
    }

    @ExceptionHandler(NoSuchElementException.class)
    public Object handleNotFound(NoSuchElementException ex, HttpServletRequest request, HandlerMethod handlerMethod) {
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage() : "요청한 항목을 찾을 수 없습니다.";
        return respond(request, handlerMethod, HttpStatus.NOT_FOUND, message, ErrorCode.RESOURCE_NOT_FOUND);
    }

    // 의도적으로 작성한 검증 메시지는 그대로 노출한다 (GlobalExceptionHandler와 동일 규칙).
    // InvalidRequestException은 IllegalArgumentException의 하위 타입이라 Spring이 이 핸들러를 먼저 선택한다.
    @ExceptionHandler(InvalidRequestException.class)
    public Object handleInvalidRequest(InvalidRequestException ex, HttpServletRequest request, HandlerMethod handlerMethod) {
        log.debug("관리자 페이지 검증 실패: {} {} — {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return respond(request, handlerMethod, HttpStatus.BAD_REQUEST, ex.getMessage(), ErrorCode.ILLEGAL_ARGUMENT);
    }

    // 순수 IllegalArgumentException(JDK·라이브러리 발) 및 파라미터 바인딩 실패 — 내부 구현이 새지 않도록
    // 일반 메시지로 응답한다. 사용자용 검증 메시지는 InvalidRequestException으로 던질 것.
    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HandlerMethodValidationException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public Object handleBadRequest(Exception ex, HttpServletRequest request, HandlerMethod handlerMethod) {
        log.debug("관리자 페이지 잘못된 요청: {} {} — {}", request.getMethod(), request.getRequestURI(), ex.toString());
        return respond(request, handlerMethod, HttpStatus.BAD_REQUEST,
                "요청 값이 올바르지 않습니다. 입력을 확인하고 다시 시도해주세요.", ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleUploadTooLarge(MaxUploadSizeExceededException ex,
                                       HttpServletRequest request, HandlerMethod handlerMethod) {
        return respond(request, handlerMethod, HttpStatus.PAYLOAD_TOO_LARGE,
                "업로드한 파일이 허용 용량을 초과했습니다.", ErrorCode.FILE_TOO_LARGE);
    }

    @ExceptionHandler(Exception.class)
    public Object handleUnexpected(Exception ex, HttpServletRequest request, HandlerMethod handlerMethod) {
        log.error("관리자 페이지 처리 중 예외: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return respond(request, handlerMethod, HttpStatus.INTERNAL_SERVER_ERROR,
                "요청을 처리하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", ErrorCode.SERVER_ERROR);
    }

    private Object respond(HttpServletRequest request, HandlerMethod handlerMethod,
                           HttpStatus status, String message, ErrorCode code) {
        if (expectsJson(handlerMethod)) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ErrorResponse.of(status, message, code));
        }
        ModelAndView mav = new ModelAndView(ERROR_VIEW);
        mav.setStatus(status);
        mav.addObject("statusCode", status.value());
        mav.addObject("statusText", status.getReasonPhrase());
        mav.addObject("errorMessage", message);
        return mav;
    }

    // @ResponseBody 메서드(또는 @RestController)면 JSON, 그 외 Thymeleaf 뷰 컨트롤러면 HTML.
    private boolean expectsJson(HandlerMethod handlerMethod) {
        if (handlerMethod == null) {
            return false;
        }
        return handlerMethod.hasMethodAnnotation(ResponseBody.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), ResponseBody.class);
    }
}
