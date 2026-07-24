package com.feple.feple_backend.certification.event;

public record CertificationApprovedEvent(
        Long userId,
        String festivalTitle,
        String festivalTitleEn,
        Long festivalId
) {}
