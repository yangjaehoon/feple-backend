package com.feple.feple_backend.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String code,
        String field
) {
    public static ErrorResponse of(HttpStatus status, String message, String code) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, code, null);
    }

    public static ErrorResponse withField(HttpStatus status, String message, String code, String field) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, code, field);
    }

    /** 필터·인터셉터에서 Jackson 없이 직접 HTTP 응답을 쓸 때 사용 */
    public static String toJson(HttpStatus status, String message, String code) {
        return "{\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":" + status.value() + ","
                + "\"error\":\"" + status.getReasonPhrase() + "\","
                + "\"message\":\"" + message + "\","
                + "\"code\":\"" + code + "\"}";
    }
}
