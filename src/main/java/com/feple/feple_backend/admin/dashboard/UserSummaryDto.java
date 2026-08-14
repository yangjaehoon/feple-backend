package com.feple.feple_backend.admin.dashboard;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.user.entity.User;

public record UserSummaryDto(
        Long id,
        String nickname,
        String email,
        String profileImageUrl
) {
    public static UserSummaryDto from(User user, FileStorageService fileStorageService) {
        return new UserSummaryDto(user.getId(), user.getNickname(), user.getEmail(),
                fileStorageService.resolveProfileImageUrl(user.getProfileImageUrl()));
    }
}
