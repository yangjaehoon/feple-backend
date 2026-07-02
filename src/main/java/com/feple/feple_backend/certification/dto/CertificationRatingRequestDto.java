package com.feple.feple_backend.certification.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CertificationRatingRequestDto(
        @NotNull(message = "별점은 필수입니다.")
        @Min(value = 1, message = "별점은 1 이상이어야 합니다.")
        @Max(value = 5, message = "별점은 5 이하이어야 합니다.")
        Integer rating,

        @Size(max = 100, message = "한줄 후기는 100자 이내로 작성해주세요.")
        String review
) {}
