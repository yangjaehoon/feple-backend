package com.feple.feple_backend.admin.certification;

import java.time.LocalDateTime;

public record CertSummaryDto(
        Long id,
        String festivalTitle,
        String userNickname,
        LocalDateTime createdAt
) {
    public static CertSummaryDto from(com.feple.feple_backend.certification.entity.FestivalCertification cert) {
        return new CertSummaryDto(cert.getId(), cert.getFestivalTitle(), cert.getUserNickname(), cert.getCreatedAt());
    }
}
