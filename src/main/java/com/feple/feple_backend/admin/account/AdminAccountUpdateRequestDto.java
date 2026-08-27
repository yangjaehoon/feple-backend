package com.feple.feple_backend.admin.account;

import static com.feple.feple_backend.admin.AdminParamDefaults.orEmpty;
import static com.feple.feple_backend.admin.AdminParamDefaults.orEmptySet;

import java.util.Set;
import org.springframework.web.multipart.MultipartFile;

public record AdminAccountUpdateRequestDto(
        String displayName,
        AdminRole role,
        // 폼에서 권한별로 "읽기" / "쓰기" 체크박스를 각각 전송한다. 쓰기는 읽기를 포함한다.
        Set<AdminPermission> readPermissions,
        Set<AdminPermission> writePermissions,
        String password,
        MultipartFile profileImage,
        Boolean deleteProfileImage
) {
    public AdminAccountUpdateRequestDto {
        displayName = orEmpty(displayName);
        readPermissions = orEmptySet(readPermissions);
        writePermissions = orEmptySet(writePermissions);
        deleteProfileImage = deleteProfileImage == null ? Boolean.FALSE : deleteProfileImage;
    }
}
