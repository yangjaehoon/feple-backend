package com.feple.feple_backend.certification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CertificationRequestDto(
        @NotNull(message = "페스티벌 ID는 필수입니다.") Long festivalId,
        @NotBlank(message = "사진 objectKey는 필수입니다.")
        @Size(max = 512, message = "photoKey가 너무 깁니다.") String photoKey
) {}
