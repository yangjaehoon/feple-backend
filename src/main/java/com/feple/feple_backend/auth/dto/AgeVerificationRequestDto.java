package com.feple.feple_backend.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;

/** 나이 확인 — 생년월일 제출. {@code "2015-04-01"} 형식(ISO-8601 LocalDate). */
public record AgeVerificationRequestDto(

        @NotNull(message = "생년월일을 입력해주세요.")
        @Past(message = "생년월일이 올바르지 않습니다.")
        LocalDate birthDate
) {}
