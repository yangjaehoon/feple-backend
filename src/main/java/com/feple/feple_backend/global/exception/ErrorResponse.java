package com.feple.feple_backend.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String code,
        String field
) {
    public static ErrorResponse of(HttpStatus status, String message, ErrorCode code) {
        return new ErrorResponse(OffsetDateTime.now(ZoneOffset.UTC), status.value(), status.getReasonPhrase(), message, code.name(), null);
    }

    public static ErrorResponse withField(HttpStatus status, String message, ErrorCode code, String field) {
        return new ErrorResponse(OffsetDateTime.now(ZoneOffset.UTC), status.value(), status.getReasonPhrase(), message, code.name(), field);
    }
}
