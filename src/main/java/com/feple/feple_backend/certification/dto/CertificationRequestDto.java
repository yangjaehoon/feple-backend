package com.feple.feple_backend.certification.dto;

import com.feple.feple_backend.global.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CertificationRequestDto(
        @NotNull(message = ValidationMessages.FESTIVAL_ID_REQUIRED) Long festivalId,
        @NotBlank(message = "사진 objectKey는 필수입니다.")
        @Size(max = 255, message = "photoKey가 너무 깁니다.") String photoKey
) {}
