package com.feple.feple_backend.certification.dto;

import jakarta.validation.constraints.NotNull;

public record CertificationReviewDto(
        @NotNull(message = "승인 여부는 필수입니다.") Boolean approved,
        String rejectionMessage
) {}
