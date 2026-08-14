package com.feple.feple_backend.userblock.service;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import com.feple.feple_backend.userblock.dto.BlockedUserDto;
import com.feple.feple_backend.userblock.entity.UserBlock;
import com.feple.feple_backend.userblock.repository.UserBlockRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserBlockServiceImpl implements UserBlockService {

    private final UserBlockRepository blockRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public void block(Long blockerId, Long targetId) {
        if (blockerId.equals(targetId)) {
            throw new IllegalArgumentException("자기 자신을 차단할 수 없습니다.");
        }
        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, targetId)) {
            throw new ConflictException("이미 차단한 사용자입니다.");
        }
        User blocker = EntityLoader.getOrThrow(userRepository::findById, blockerId, "사용자");
        User blocked = EntityLoader.getOrThrow(userRepository::findById, targetId, "사용자");
        try {
            blockRepository.save(UserBlock.of(blocker, blocked));
            blockRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("이미 차단한 사용자입니다.");
        }
    }

    // ArtistFollowServiceImpl.unfollow()와 동일하게, 대상 관계가 이미 없는 상태에서의 삭제 요청은
    // 예외가 아닌 멱등한 no-op으로 처리한다(중복 탭 등으로 인한 재요청에도 안전).
    @Override
    @Transactional
    public void unblock(Long blockerId, Long targetId) {
        blockRepository.deleteByBlockerIdAndBlockedId(blockerId, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BlockedUserDto> getBlockedUsers(Long blockerId) {
        return blockRepository.findByBlockerIdOrderByCreatedAtDesc(blockerId)
                .stream()
                .map(b -> BlockedUserDto.from(b, fileStorageService))
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
