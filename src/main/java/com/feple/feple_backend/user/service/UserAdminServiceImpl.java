package com.feple.feple_backend.user.service;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.global.JpqlLikeEscaper;
import com.feple.feple_backend.global.PageableFactory;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final UserCascadeDeleteService cascadeDeleteService;

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getAdminUser(@NonNull Long id) {
        User user = EntityLoader.getOrThrow(userRepository::findById, id, "사용자");
        return toAdminUserDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto findByNickname(String nickname) {
        User user = EntityLoader.getOrThrow(userRepository::findByNicknameAndNotDeleted, nickname.trim(), "사용자");
        return toAdminUserDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getUsersPage(int page, int size, String keyword) {
        Pageable pageable = PageableFactory.orderByLatestId(page, size);
        if (keyword != null && !keyword.isBlank()) {
            return userRepository.findActiveByKeyword(JpqlLikeEscaper.escape(keyword.trim()), pageable).map(this::toAdminUserDto);
        }
        return userRepository.findAllByDeletedAtIsNull(pageable).map(this::toAdminUserDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getUsersPageSortedByReports(int page, int size, String keyword) {
        String kw = JpqlLikeEscaper.escapeOrEmpty(keyword);
        return userRepository.findAllOrderByTotalReportCountDesc(kw, PageRequest.of(page, size))
                .map(this::toAdminUserDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getBannedUsersPage(int page, int size, String keyword) {
        String kw = JpqlLikeEscaper.escapeOrEmpty(keyword);
        return userRepository.findBannedUsers(LocalDateTime.now(), kw, PageRequest.of(page, size))
                .map(this::toAdminUserDto);
    }

    @Override
    @Transactional
    public void bulkDeleteUsers(List<Long> ids) {
        userRepository.findAllById(ids).forEach(cascadeDeleteService::delete);
    }

    @Override
    public String adminDeleteUser(@NonNull Long id) {
        User user = EntityLoader.getOrThrow(userRepository::findById, id, "사용자");
        String nickname = user.getNickname();
        cascadeDeleteService.delete(user);
        return nickname;
    }

    @Override
    public void updateUserRole(Long userId, UserRole role) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        user.changeRole(role);
    }

    @Override
    public void banUser(Long userId, int days, String reason) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        String adminUsername = null;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) adminUsername = auth.getName();
        user.ban(days, reason, adminUsername);
    }

    @Override
    public void unbanUser(Long userId) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        user.unban();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsersForExport() {
        List<UserResponseDto> result = new ArrayList<>();
        int page = 0;
        final int batchSize = 1000;
        Page<User> batch;
        do {
            batch = userRepository.findAllByDeletedAtIsNull(
                    PageableFactory.orderByLatestId(page++, batchSize));
            batch.forEach(u -> result.add(toAdminUserDto(u)));
        } while (batch.hasNext() && result.size() < AdminConstants.MAX_EXPORT_ROWS);
        return result;
    }

    private UserResponseDto toAdminUserDto(User user) {
        return UserResponseMapper.toAdminUserDto(user, fileStorageService);
    }
}
