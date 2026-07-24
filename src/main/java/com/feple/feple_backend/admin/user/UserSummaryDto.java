package com.feple.feple_backend.admin.user;

public record UserSummaryDto(
        Long id,
        String nickname,
        String email,
        String profileImageUrl
) {
    public static UserSummaryDto from(com.feple.feple_backend.user.entity.User user) {
        return new UserSummaryDto(user.getId(), user.getNickname(), user.getEmail(), user.getProfileImageUrl());
    }
}
