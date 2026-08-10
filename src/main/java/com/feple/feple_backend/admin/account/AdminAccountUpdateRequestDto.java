package com.feple.feple_backend.admin.account;

import static com.feple.feple_backend.admin.AdminParamDefaults.orEmpty;
import static com.feple.feple_backend.admin.AdminParamDefaults.orEmptySet;

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
        displayName = orEmpty(displayName);
        permissions = orEmptySet(permissions);
        deleteProfileImage = deleteProfileImage == null ? Boolean.FALSE : deleteProfileImage;
    }
}
