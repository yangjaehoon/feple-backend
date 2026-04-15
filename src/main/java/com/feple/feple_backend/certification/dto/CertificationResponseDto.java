package com.feple.feple_backend.certification.dto;

import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;

import java.time.LocalDateTime;

public record CertificationResponseDto(
        Long id,
        Long festivalId,
        String festivalTitle,
        String festivalPosterUrl,
        String photoUrl,
        CertificationStatus status,
        String rejectionMessage,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt
) {
    public static CertificationResponseDto from(FestivalCertification cert, String festivalPosterUrl, String photoUrl) {
        return new CertificationResponseDto(
                cert.getId(),
                cert.getFestival().getId(),
                cert.getFestival().getTitle(),
                festivalPosterUrl,
                photoUrl,
                cert.getStatus(),
                cert.getRejectionMessage(),
                cert.getCreatedAt(),
                cert.getReviewedAt()
        );
    }
}
