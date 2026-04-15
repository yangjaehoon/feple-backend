package com.feple.feple_backend.notification.entity;

public enum NotificationType {
    NEW_FESTIVAL,      // 팔로우 아티스트 신규 페스티벌
    CERT_APPROVED,     // 인증 승인
    CERT_REJECTED,     // 인증 거절
    NEW_COMMENT,       // 내 게시글에 댓글
    FESTIVAL_REMINDER  // 페스티벌 D-7 / D-1 리마인더
}
