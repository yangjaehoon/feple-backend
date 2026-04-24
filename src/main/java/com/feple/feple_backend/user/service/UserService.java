package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageLikeRepository;
import com.feple.feple_backend.artist.photo.repository.ArtistProfileImageRepository;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.global.exception.AuthenticationRequiredException;
import com.feple.feple_backend.notification.repository.NotificationRepository;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.NicknameValidator;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.repository.UserDeviceTokenRepository;
import com.feple.feple_backend.user.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.feple.feple_backend.file.service.FileStorageService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final ArtistFollowRepository artistFollowRepository;
    private final FestivalLikeRepository festivalLikeRepository;
    private final FileStorageService fileStorageService;
    private final NotificationRepository notificationRepository;
    private final UserDeviceTokenRepository userDeviceTokenRepository;
    private final FestivalCertificationRepository certificationRepository;
    private final ArtistProfileImageLikeRepository artistImageLikeRepository;
    private final ArtistProfileImageRepository artistImageRepository;

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> checkNicknameAvailable(String nickname, Long excludeUserId) {
        try {
            NicknameValidator.validate(nickname);
        } catch (IllegalArgumentException e) {
            return java.util.Map.of("available", false, "message", e.getMessage());
        }
        boolean taken = excludeUserId != null
                ? userRepository.existsByNicknameAndIdNot(nickname.trim(), excludeUserId)
                : userRepository.existsByNickname(nickname.trim());
        if (taken) {
            return java.util.Map.of("available", false, "message", "이미 사용 중인 닉네임입니다.");
        }
        return java.util.Map.of("available", true, "message", "사용 가능한 닉네임입니다.");
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUser(@NonNull Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + id));
        return toUserDto(user);
    }

    /** 관리자 페이지용: email 포함 */
    @Transactional(readOnly = true)
    public UserResponseDto getAdminUser(@NonNull Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + id));
        return toAdminUserDto(user);
    }

    @Transactional
    public UserResponseDto updateNickname(@NonNull Long id, String nickname) {
        NicknameValidator.validate(nickname);
        if (userRepository.existsByNicknameAndIdNot(nickname.trim(), id)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + id));
        user.changeNickname(nickname.trim());
        return toUserDto(user);
    }

    @Transactional
    public UserResponseDto updateProfileImage(@NonNull Long id, MultipartFile file) throws java.io.IOException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + id));
        String url = fileStorageService.storeUserProfile(file, user.getNickname());
        user.changeProfileImage(url);
        return toUserDto(user);
    }

    public void deleteUser(@NonNull Long id) {
        adminDeleteUser(id);
    }

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

    @Transactional
    public void bulkDeleteUsers(List<Long> ids) {
        for (Long id : ids) {
            adminDeleteUser(id);
        }
    }

    @Transactional
    public void adminDeleteUser(@NonNull Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + id));
        String profileImageKey = user.getProfileImageUrl();

        // 1. 사용자의 댓글 삭제
        commentRepository.deleteAll(commentRepository.findByUser(user));

        // 2. 사용자 게시글의 좋아요 삭제
        List<Post> userPosts = postRepository.findByUser(user);
        for (Post post : userPosts) {
            postLikeRepository.deleteByPostId(post.getId());
        }

        // 3. 사용자가 누른 좋아요 삭제
        postLikeRepository.deleteByUser(user);

        // 4. 사용자 게시글 삭제 (cascade: 댓글도 삭제)
        postRepository.deleteAll(userPosts);

        // 5. 페스티벌 좋아요, 아티스트 팔로우 삭제
        festivalLikeRepository.deleteAll(festivalLikeRepository.findByUserId(id));
        artistFollowRepository.deleteAll(artistFollowRepository.findByUserId(id));

        // 6. 알림, 디바이스 토큰, 인증 신청 삭제
        notificationRepository.deleteByUserId(id);
        userDeviceTokenRepository.deleteAll(userDeviceTokenRepository.findByUserId(id));
        certificationRepository.deleteByUserId(id);

        // 7. 아티스트 이미지 좋아요 삭제, 업로더 참조 해제
        artistImageLikeRepository.deleteByUserId(id);
        artistImageRepository.nullifyUploaderByUserId(id);

        // 8. 사용자 삭제
        userRepository.delete(user);

        // 9. S3 프로필 이미지 삭제 (DB 삭제 성공 후 실행)
        fileStorageService.deleteFile(profileImageKey);
    }

    /** null/빈값/기본이미지 → null 반환, S3 key → 전체 URL 변환 */
    public UserResponseDto toUserDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(resolveProfileImageUrl(user.getProfileImageUrl()))
                .role(user.getRole())
                .build();
    }

    /** 관리자 페이지용: email 포함 */
    public UserResponseDto toAdminUserDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(resolveProfileImageUrl(user.getProfileImageUrl()))
                .role(user.getRole())
                .build();
    }

    @Transactional
    public void updateUserRole(Long userId, com.feple.feple_backend.user.entity.UserRole role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + userId));
        user.changeRole(role);
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

    public Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AuthenticationRequiredException("로그인이 필요합니다.");
        }
        String principal = auth.getPrincipal().toString();
        return Long.parseLong(principal);
    }

}
