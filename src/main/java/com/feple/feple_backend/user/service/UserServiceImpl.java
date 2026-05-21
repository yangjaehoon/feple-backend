package com.feple.feple_backend.user.service;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.exception.AuthenticationRequiredException;
import com.feple.feple_backend.user.NicknameValidator;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService, UserAdminService {

    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final UserCascadeDeleteService cascadeDeleteService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> checkNicknameAvailable(String nickname, Long excludeUserId) {
        try {
            NicknameValidator.validate(nickname);
        } catch (IllegalArgumentException e) {
            return Map.of("available", false, "message", e.getMessage());
        }
        boolean taken = excludeUserId != null
                ? userRepository.existsByNicknameAndIdNot(nickname.trim(), excludeUserId)
                : userRepository.existsByNickname(nickname.trim());
        if (taken) {
            return Map.of("available", false, "message", "이미 사용 중인 닉네임입니다.");
        }
        return Map.of("available", true, "message", "사용 가능한 닉네임입니다.");
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getUser(@NonNull Long id) {
        User user = EntityFinder.getOrThrow(userRepository::findById, id, "사용자");
        return toUserDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getAdminUser(@NonNull Long id) {
        User user = EntityFinder.getOrThrow(userRepository::findById, id, "사용자");
        return toAdminUserDto(user);
    }

    @Override
    public UserResponseDto updateNickname(@NonNull Long id, String nickname) {
        NicknameValidator.validate(nickname);
        if (userRepository.existsByNicknameAndIdNot(nickname.trim(), id)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        User user = EntityFinder.getOrThrow(userRepository::findById, id, "사용자");
        user.changeNickname(nickname.trim());
        return toUserDto(user);
    }

    @Override
    public UserResponseDto updateProfileImage(@NonNull Long id, MultipartFile file) throws java.io.IOException {
        User user = EntityFinder.getOrThrow(userRepository::findById, id, "사용자");
        String url = fileStorageService.storeUserProfile(file, user.getNickname());
        user.changeProfileImage(url);
        return toUserDto(user);
    }

    @Override
    public void deleteUser(@NonNull Long id) {
        adminDeleteUser(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getUsersPage(int page, int size, String keyword) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        if (keyword != null && !keyword.isBlank()) {
            return userRepository
                    .findByNicknameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword, pageable)
                    .map(this::toAdminUserDto);
        }
        return userRepository.findAll(pageable).map(this::toAdminUserDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getUsersPageSortedByReports(int page, int size, String keyword) {
        String kw = (keyword == null) ? "" : keyword.trim();
        return userRepository.findAllOrderByTotalReportCountDesc(kw, PageRequest.of(page, size))
                .map(this::toAdminUserDto);
    }

    @Override
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
                .build();
    }

    private UserResponseDto toAdminUserDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(resolveProfileImageUrl(user.getProfileImageUrl()))
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public void updateUserRole(Long userId, UserRole role) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        user.changeRole(role);
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
