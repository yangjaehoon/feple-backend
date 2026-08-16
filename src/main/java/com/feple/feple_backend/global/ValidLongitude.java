package com.feple.feple_backend.global;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 경도 범위 검증을 필드별로 반복 정의하지 않도록 묶은 합성 제약 어노테이션
// (BoothRequestDto, FestivalRequestDto 등 좌표를 받는 모든 DTO가 공유)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@DecimalMin(value = "-180.0", message = "경도는 -180.0 이상이어야 합니다.")
@DecimalMax(value = "180.0", message = "경도는 180.0 이하여야 합니다.")
public @interface ValidLongitude {
    String message() default "유효하지 않은 경도입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
