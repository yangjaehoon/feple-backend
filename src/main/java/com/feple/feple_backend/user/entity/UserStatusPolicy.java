package com.feple.feple_backend.user.entity;

import java.time.LocalDateTime;

/**
 * User 엔티티와 UserResponseDto가 각자 보관하던 상태 판정 로직(관리자/아티스트 여부, 정지 여부,
 * 영구정지 판정연도)을 한 곳으로 모은다.
 */
public final class UserStatusPolicy {

    public static final int PERMANENT_BAN_YEAR = 9999;

    private UserStatusPolicy() {}

    public static boolean isAdmin(UserRole role) {
        return role == UserRole.ADMIN;
    }

    public static boolean isArtist(UserRole role) {
        return role == UserRole.ARTIST;
    }

    public static boolean isBanned(LocalDateTime bannedUntil) {
        return bannedUntil != null && bannedUntil.isAfter(LocalDateTime.now());
    }

    public static boolean isPermanentBan(LocalDateTime bannedUntil) {
        return bannedUntil != null && bannedUntil.getYear() >= PERMANENT_BAN_YEAR;
    }

    public static LocalDateTime permanentBanUntil() {
        return LocalDateTime.of(PERMANENT_BAN_YEAR, 12, 31, 23, 59, 59);
    }
}
