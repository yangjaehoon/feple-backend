package com.feple.feple_backend.userblock.dto;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.userblock.entity.UserBlock;
import java.time.LocalDateTime;
import lombok.Getter;

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

    public static BlockedUserDto from(UserBlock block, FileStorageService fileStorageService) {
        var blocked = block.getBlocked();
        return new BlockedUserDto(
                block.getBlockedId(),
                blocked.getNickname(),
                fileStorageService.resolveProfileImageUrl(blocked.getProfileImageUrl()),
                block.getCreatedAt()
        );
    }
}
