package com.feple.feple_backend.userblock.service;

import com.feple.feple_backend.userblock.dto.BlockedUserDto;

import java.util.List;

public interface UserBlockService {
    void block(Long blockerId, Long targetId);
    void unblock(Long blockerId, Long targetId);
    List<BlockedUserDto> getBlockedUsers(Long blockerId);
    boolean isBlocked(Long blockerId, Long targetId);
    List<Long> getBlockedIds(Long blockerId);
    void removeAllByUser(Long userId);
}
