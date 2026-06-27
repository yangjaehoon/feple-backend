package com.feple.feple_backend.admin.account;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public record AdminAccountUpdateRequestDto(
        String displayName,
        AdminRole role,
        Set<AdminPermission> permissions,
        String newPassword,
        MultipartFile profileImage,
        boolean deleteProfileImage
) {}
