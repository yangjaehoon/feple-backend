package com.feple.feple_backend.admin.account;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminRole {

    SUPER_ADMIN("최고 관리자"),
    MANAGER("일반 관리자");

    private final String displayName;
}
