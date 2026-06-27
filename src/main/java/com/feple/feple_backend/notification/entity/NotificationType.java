package com.feple.feple_backend.notification.entity;

public enum NotificationType {
    NEW_FESTIVAL,      // 팔로우 아티스트 신규 페스티벌
    CERT_APPROVED,     // 인증 승인
    CERT_REJECTED,     // 인증 거절
    NEW_COMMENT,           // 내 게시글에 댓글
    NEW_REPLY,             // 내 댓글에 대댓글
    POST_LIKED,            // 내 게시글에 좋아요
    POST_DELETED_BY_ADMIN, // 관리자에 의한 게시글 삭제
    FESTIVAL_REMINDER,     // 페스티벌 D-7 / D-1 리마인더
    SONG_REQUEST_APPROVED,       // 노래 요청 승인
    SONG_REQUEST_REJECTED,       // 노래 요청 거절
    ARTIST_SUGGESTION_PROCESSED, // 아티스트 신청 처리 결과
    ADMIN_BROADCAST              // 관리자 전체 공지
}
