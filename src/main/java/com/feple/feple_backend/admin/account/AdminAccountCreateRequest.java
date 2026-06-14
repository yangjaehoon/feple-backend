package com.feple.feple_backend.admin.account;

import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

public record AdminAccountCreateRequest(
        String username,
        String password,
        String displayName,
        AdminRole role,
        Set<AdminPermission> permissions,
        MultipartFile profileImage
) {}
