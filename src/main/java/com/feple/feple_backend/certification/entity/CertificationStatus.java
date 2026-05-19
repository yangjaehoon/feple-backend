package com.feple.feple_backend.certification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CertificationStatus {
    PENDING("대기중"),
    APPROVED("승인"),
    REJECTED("거절");

    private final String displayName;
}
