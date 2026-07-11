package com.feple.feple_backend.userblock.service;

import com.feple.feple_backend.global.EntityRequirer;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.dto.BlockedUserDto;
import com.feple.feple_backend.userblock.entity.UserBlock;
import com.feple.feple_backend.userblock.repository.UserBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserBlockServiceImpl implements UserBlockService {

    private final UserBlockRepository blockRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void block(Long blockerId, Long targetId) {
        if (blockerId.equals(targetId)) {
            throw new IllegalArgumentException("자기 자신을 차단할 수 없습니다.");
        }
        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, targetId)) {
            throw new ConflictException("이미 차단한 사용자입니다.");
        }
        User blocker = EntityRequirer.getOrThrow(userRepository::findById, blockerId, "사용자");
        User blocked = EntityRequirer.getOrThrow(userRepository::findById, targetId, "사용자");
        blockRepository.save(UserBlock.of(blocker, blocked));
    }

    @Override
    @Transactional
    public void unblock(Long blockerId, Long targetId) {
        int deleted = blockRepository.deleteByBlockerIdAndBlockedId(blockerId, targetId);
        if (deleted == 0) {
            throw new IllegalArgumentException("차단 내역이 없습니다.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockedUserDto> getBlockedUsers(Long blockerId) {
        return blockRepository.findByBlockerIdOrderByCreatedAtDesc(blockerId)
                .stream()
                .map(BlockedUserDto::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(Long blockerId, Long targetId) {
        return blockRepository.existsByBlockerIdAndBlockedId(blockerId, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getBlockedIds(Long blockerId) {
        return blockRepository.findBlockedIdsByBlockerId(blockerId);
    }

    @Override
    @Transactional
    public void removeAllByUser(Long userId) {
        blockRepository.deleteAllByUserId(userId);
    }
}
