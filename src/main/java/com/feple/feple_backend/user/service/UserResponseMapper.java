package com.feple.feple_backend.user.service;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;

final class UserResponseMapper {

    private UserResponseMapper() {}

    static UserResponseDto toUserDto(User user, FileStorageService fileStorageService) {
        return UserResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(resolveProfileImageUrl(user.getProfileImageUrl(), fileStorageService))
                .role(user.getRole())
                .bio(user.getBio())
                .level(user.getLevel().name())
                .nicknameChangedAt(user.getNicknameChangedAt())
                .build();
    }

    static UserResponseDto toAdminUserDto(User user, FileStorageService fileStorageService) {
        return UserResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .bio(user.getBio())
                .profileImageUrl(resolveProfileImageUrl(user.getProfileImageUrl(), fileStorageService))
                .role(user.getRole())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                .bannedUntil(user.getBannedUntil())
                .banReason(user.getBanReason())
                .bannedBy(user.getBannedBy())
                .deletedAt(user.getDeletedAt())
                .build();
    }

    private static String resolveProfileImageUrl(String raw, FileStorageService fileStorageService) {
        if (raw == null || raw.isBlank() || raw.contains("/img/feple_logo.png")) {
            return null;
        } else if (raw.startsWith("http")) {
            return raw;
        } else {
            return fileStorageService.buildUrl(raw);
        }
    }
}
