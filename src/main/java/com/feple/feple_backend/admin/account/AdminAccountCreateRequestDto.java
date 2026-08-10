package com.feple.feple_backend.admin.account;

import static com.feple.feple_backend.admin.AdminParamDefaults.orEmpty;
import static com.feple.feple_backend.admin.AdminParamDefaults.orEmptySet;

import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

public record AdminAccountCreateRequestDto(
        String username,
        String password,
        String displayName,
        AdminRole role,
        Set<AdminPermission> permissions,
        MultipartFile profileImage
) {
    public AdminAccountCreateRequestDto {
        displayName = orEmpty(displayName);
        permissions = orEmptySet(permissions);
    }
}
