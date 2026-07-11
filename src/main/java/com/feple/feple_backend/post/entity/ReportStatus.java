package com.feple.feple_backend.post.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportStatus {
    PENDING("대기"),
    POST_DELETED("삭제됨"),
    REJECTED("기각");

    private final String displayName;

    public static ReportStatus fromFilter(String statusFilter) {
        return "PENDING".equals(statusFilter) ? PENDING : null;
    }
}
