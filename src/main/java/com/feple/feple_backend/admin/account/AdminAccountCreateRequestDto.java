package com.feple.feple_backend.admin.account;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public record AdminAccountCreateRequestDto(
        String username,
        String password,
        String displayName,
        AdminRole role,
        Set<AdminPermission> permissions,
        MultipartFile profileImage
) {
    public AdminAccountCreateRequestDto {
        displayName = displayName == null ? "" : displayName;
        permissions = permissions == null ? Set.of() : permissions;
    }
}
