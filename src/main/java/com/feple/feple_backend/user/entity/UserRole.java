package com.feple.feple_backend.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    USER("일반 사용자"),
    ARTIST("아티스트"),
    ADMIN("관리자");

    private final String displayName;
}
