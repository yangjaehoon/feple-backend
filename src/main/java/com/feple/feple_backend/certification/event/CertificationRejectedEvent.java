package com.feple.feple_backend.certification.event;

public record CertificationRejectedEvent(
        Long userId,
        String festivalTitle,
        String festivalTitleEn,
        Long festivalId,
        String reason
) {}
