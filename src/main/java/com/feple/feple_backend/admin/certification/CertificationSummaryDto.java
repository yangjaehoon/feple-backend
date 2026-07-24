package com.feple.feple_backend.admin.certification;

import java.time.LocalDateTime;

public record CertificationSummaryDto(
        Long id,
        String festivalTitle,
        String userNickname,
        LocalDateTime createdAt
) {
    public static CertificationSummaryDto from(com.feple.feple_backend.certification.entity.FestivalCertification cert) {
        return new CertificationSummaryDto(cert.getId(), cert.getFestivalTitle(), cert.getUserNickname(), cert.getCreatedAt());
    }
}
