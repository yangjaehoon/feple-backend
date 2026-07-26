package com.feple.feple_backend.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointReason {
    POST_CREATED("게시글 작성"),
    COMMENT_CREATED("댓글 작성"),
    POST_LIKED_RECEIVED("좋아요 받음"),
    CERT_APPROVED("인증 승인"),
    POST_DELETED_BY_ADMIN("게시글 관리자 삭제"),
    ADMIN_GRANTED("관리자 지급");

    private final String displayName;
}
