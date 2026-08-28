package com.feple.feple_backend.admin.account;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AdminRole {

    SUPER_ADMIN("최고 관리자"),
    MANAGER("일반 관리자");

    private final String displayName;

    /** Spring Security 권한 문자열 (예: SUPER_ADMIN → "ROLE_SUPER_ADMIN"). */
    public String authority() {
        return "ROLE_" + name();
    }
}
