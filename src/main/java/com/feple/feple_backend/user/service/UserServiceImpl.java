package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.ArtistNameFilter;
import com.feple.feple_backend.badword.BadWordFilter;
import com.feple.feple_backend.nickname.NicknameRestrictionFilter;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.LikeEscaper;
import com.feple.feple_backend.global.exception.AuthenticationRequiredException;
import com.feple.feple_backend.global.exception.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import com.feple.feple_backend.user.NicknameValidator;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.feple.feple_backend.global.PageableFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService, UserAdminService {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final UserCascadeDeleteService cascadeDeleteService;
    private final BadWordFilter badWordFilter;
    private final ArtistNameFilter artistNameFilter;
    private final NicknameRestrictionFilter nicknameRestrictionFilter;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> checkNicknameAvailable(String nickname, Long excludeUserId) {
        try {
            NicknameValidator.validate(nickname);
        } catch (IllegalArgumentException e) {
            return Map.of("available", false, "code", "INVALID_FORMAT", "message", e.getMessage());
        }
        try {
            badWordFilter.validate(nickname);
        } catch (IllegalArgumentException e) {
            return Map.of("available", false, "code", "BAD_WORD", "message", e.getMessage());
        }
        try {
            artistNameFilter.validate(nickname);
        } catch (IllegalArgumentException e) {
            return Map.of("available", false, "code", "ARTIST_NAME", "message", e.getMessage());
        }
        try {
            nicknameRestrictionFilter.validate(nickname);
        } catch (IllegalArgumentException e) {
            return Map.of("available", false, "code", "RESTRICTED", "message", e.getMessage());
        }
        boolean taken = excludeUserId != null
                ? userRepository.existsByNicknameAndIdNot(nickname.trim(), excludeUserId)
                : userRepository.existsByNickname(nickname.trim());
        if (taken) {
            return Map.of("available", false, "code", "DUPLICATE", "message", "이미 사용 중인 닉네임입니다.");
        }
        return Map.of("available", true, "code", "AVAILABLE", "message", "사용 가능한 닉네임입니다.");
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUser(@NonNull Long id) {
        User user = EntityFinder.getOrThrow(userRepository::findById, id, "사용자");
        return toUserDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto findByNickname(String nickname) {
        User user = EntityFinder.getOrThrow(userRepository::findByNicknameAndNotDeleted, nickname.trim(), "사용자");
        return toUserDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getAdminUser(@NonNull Long id) {
        User user = EntityFinder.getOrThrow(userRepository::findById, id, "사용자");
        return toAdminUserDto(user);
    }

    @Override
    public void updateNickname(@NonNull Long id, String nickname) {
        NicknameValidator.validate(nickname);
        badWordFilter.validateField("nickname", nickname);
        artistNameFilter.validate(nickname);
        nicknameRestrictionFilter.validate(nickname);
        if (userRepository.existsByNicknameAndIdNot(nickname.trim(), id)) {
            throw new ConflictException("이미 사용 중인 닉네임입니다.");
        }
        User user = EntityFinder.getOrThrow(userRepository::findById, id, "사용자");
        if (!user.canChangeNickname()) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), user.nextNicknameChangeAt()) + 1;
            throw new IllegalArgumentException("닉네임은 90일에 한 번만 변경할 수 있습니다. " + daysLeft + "일 후에 변경 가능합니다.");
        }
        user.changeNickname(nickname.trim());
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("이미 사용 중인 닉네임입니다.");
        }
    }

    @Override
    public void updateBio(@NonNull Long id, String bio) {
        if (bio != null) badWordFilter.validateField("bio", bio);
        User user = EntityFinder.getOrThrow(userRepository::findById, id, "사용자");
        user.updateBio(bio);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void updateProfileImage(@NonNull Long id, MultipartFile file) {
        try {
            // S3 업로드는 커넥션 점유 없이 수행; 완료 후 별도 트랜잭션으로 DB 반영
            User user = EntityFinder.getOrThrow(userRepository::findById, id, "사용자");
            String url = fileStorageService.storeUserProfile(file, user.getNickname());
            user.changeProfileImage(url);
            userRepository.save(user);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("프로필 이미지 저장에 실패했습니다.", e);
        }
    }

    @Override
    public void deleteUser(@NonNull Long id) {
        adminDeleteUser(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getUsersPage(int page, int size, String keyword) {
        Pageable pageable = PageableFactory.newestId(page, size);
        if (keyword != null && !keyword.isBlank()) {
            return userRepository.findActiveByKeyword(LikeEscaper.escape(keyword.trim()), pageable).map(this::toAdminUserDto);
        }
        return userRepository.findAllByDeletedAtIsNull(pageable).map(this::toAdminUserDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getUsersPageSortedByReports(int page, int size, String keyword) {
        String kw = LikeEscaper.escapeOrEmpty(keyword);
        return userRepository.findAllOrderByTotalReportCountDesc(kw, PageRequest.of(page, size))
                .map(this::toAdminUserDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getBannedUsersPage(int page, int size, String keyword) {
        String kw = LikeEscaper.escapeOrEmpty(keyword);
        return userRepository.findBannedUsers(LocalDateTime.now(), kw, PageRequest.of(page, size))
                .map(this::toAdminUserDto);
    }

    @Override
    @Transactional
    public void bulkDeleteUsers(List<Long> ids) {
        for (Long id : ids) {
            adminDeleteUser(id);
        }
    }

    @Override
    public void adminDeleteUser(@NonNull Long id) {
        User user = EntityFinder.getOrThrow(userRepository::findById, id, "사용자");
        cascadeDeleteService.delete(user);
    }

    @Override
    public UserResponseDto toUserDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(resolveProfileImageUrl(user.getProfileImageUrl()))
                .role(user.getRole())
                .bio(user.getBio())
                .level(user.getLevel().name())
                .nicknameChangedAt(user.getNicknameChangedAt())
                .build();
    }

    private UserResponseDto toAdminUserDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .bio(user.getBio())
                .profileImageUrl(resolveProfileImageUrl(user.getProfileImageUrl()))
                .role(user.getRole())
                .provider(user.getProvider())
                .createdAt(user.getCreatedAt())
                .bannedUntil(user.getBannedUntil())
                .banReason(user.getBanReason())
                .bannedBy(user.getBannedBy())
                .deletedAt(user.getDeletedAt())
                .build();
    }

    @Override
    public void updateUserRole(Long userId, UserRole role) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        user.changeRole(role);
    }

    @Override
    public void banUser(Long userId, int days, String reason) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        String adminUsername = null;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) adminUsername = auth.getName();
        user.ban(days, reason, adminUsername);
    }

    @Override
    public void unbanUser(Long userId) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
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
                    PageableFactory.newestId(page++, batchSize));
            batch.forEach(u -> result.add(toAdminUserDto(u)));
        } while (batch.hasNext());
        return result;
    }

    @Override
    public Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AuthenticationRequiredException("로그인이 필요합니다.");
        }
        return Long.parseLong(auth.getPrincipal().toString());
    }

    private String resolveProfileImageUrl(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("/img/feple_logo.png")) {
            return null;
        } else if (raw.startsWith("http")) {
            return raw;
        } else {
            return fileStorageService.buildUrl(raw);
        }
    }
}
