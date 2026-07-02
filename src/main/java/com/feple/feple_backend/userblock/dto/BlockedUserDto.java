package com.feple.feple_backend.userblock.dto;

import com.feple.feple_backend.userblock.entity.UserBlock;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BlockedUserDto {
    private final Long userId;
    private final String nickname;
    private final String profileImageUrl;
    private final LocalDateTime blockedAt;

    private BlockedUserDto(Long userId, String nickname, String profileImageUrl, LocalDateTime blockedAt) {
        this.userId = userId;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.blockedAt = blockedAt;
    }

    public static BlockedUserDto from(UserBlock block) {
        var blocked = block.getBlocked();
        return new BlockedUserDto(
                blocked.getId(),
                blocked.getNickname(),
                blocked.getProfileImageUrl(),
                block.getCreatedAt()
        );
    }
}
