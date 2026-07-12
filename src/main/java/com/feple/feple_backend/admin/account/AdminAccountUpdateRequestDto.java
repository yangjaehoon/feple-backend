package com.feple.feple_backend.admin.account;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public record AdminAccountUpdateRequestDto(
        String displayName,
        AdminRole role,
        Set<AdminPermission> permissions,
        String password,
        MultipartFile profileImage,
        Boolean deleteProfileImage
) {
    public AdminAccountUpdateRequestDto {
        displayName = displayName == null ? "" : displayName;
        permissions = permissions == null ? Set.of() : permissions;
        deleteProfileImage = deleteProfileImage == null ? false : deleteProfileImage;
    }
}
