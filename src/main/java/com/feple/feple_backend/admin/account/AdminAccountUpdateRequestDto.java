package com.feple.feple_backend.admin.account;

import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

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
        deleteProfileImage = deleteProfileImage == null ? Boolean.FALSE : deleteProfileImage;
    }
}
