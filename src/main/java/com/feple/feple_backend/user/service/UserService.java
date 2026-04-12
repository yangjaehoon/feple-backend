package com.feple.feple_backend.user.service;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.repository.FestivalLikeRepository;
import com.feple.feple_backend.comment.dto.MyCommentResponseDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.post.repository.PostLikeRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.user.domain.User;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.dto.UserStatsDto;
import com.feple.feple_backend.user.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import com.feple.feple_backend.file.FileStorageService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

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

    private static final java.util.regex.Pattern NICKNAME_PATTERN = java.util.regex.Pattern
            .compile("^[가-힣a-zA-Z0-9_]+$");

    private void validateNickname(String nickname, Long excludeUserId) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }
        String trimmed = nickname.trim();
        if (trimmed.length() < 2 || trimmed.length() > 8) {
            throw new IllegalArgumentException("닉네임은 2자 이상 8자 이하로 입력해주세요.");
        }
        if (!NICKNAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("닉네임은 한글, 영문, 숫자, 밑줄(_)만 사용할 수 있습니다.");
        }
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> checkNicknameAvailable(String nickname, Long excludeUserId) {
        try {
            validateNickname(nickname, excludeUserId);
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
        validateNickname(nickname, id);
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

        // 1. 사용자의 댓글 삭제 (JPA 영속성 컨텍스트에서 REMOVED 상태로 전환)
        commentRepository.deleteAll(commentRepository.findByUser(user));

        // 2. 사용자 게시글의 좋아요 삭제
        List<Post> userPosts = postRepository.findByUser(user);
        for (Post post : userPosts) {
            postLikeRepository.deleteByPostId(post.getId());
        }

        // 3. 사용자가 누른 좋아요 삭제
        postLikeRepository.deleteByUser(user);

        // 4. 사용자 게시글 삭제 (cascade: 다른 사용자의 댓글도 삭제)
        postRepository.deleteAll(userPosts);

        // 5. 페스티벌 좋아요, 아티스트 팔로우 삭제
        festivalLikeRepository.deleteAll(festivalLikeRepository.findByUserId(id));
        artistFollowRepository.deleteAll(artistFollowRepository.findByUserId(id));

        // 6. S3 프로필 이미지 삭제
        fileStorageService.deleteFile(user.getProfileImageUrl());

        // 7. 사용자 삭제
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getMyPosts(@NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + userId));
        return postRepository.findByUser(user).stream()
                .map(PostResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MyCommentResponseDto> getMyComments(@NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + userId));
        return commentRepository.findByUser(user).stream()
                .map(MyCommentResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FestivalResponseDto> getLikedFestivals(@NonNull Long userId) {
        return festivalLikeRepository.findByUserId(userId).stream()
                .map(like -> FestivalResponseDto.from(
                        like.getFestival(),
                        fileStorageService.buildUrl(like.getFestival().getPosterKey())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ArtistResponseDto> getFollowedArtists(@NonNull Long userId) {
        return artistFollowRepository.findByUserId(userId).stream()
                .map(follow -> ArtistResponseDto.from(
                        follow.getArtist(),
                        fileStorageService.buildUrl(follow.getArtist().getProfileImageKey())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserStatsDto getUserStats(@NonNull Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + userId));
        long postCount = postRepository.countByUser(user);
        long commentCount = commentRepository.countByUser(user);
        return new UserStatsDto(postCount, commentCount);
    }

    /** null/빈값/기본이미지 → null 반환, S3 key → 전체 URL 변환 */
    public UserResponseDto toUserDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(resolveProfileImageUrl(user.getProfileImageUrl()))
                .build();
    }

    /** 관리자 페이지용: email 포함 */
    public UserResponseDto toAdminUserDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .profileImageUrl(resolveProfileImageUrl(user.getProfileImageUrl()))
                .build();
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
            throw new IllegalStateException("Not authenticated");
        }
        String principal = auth.getPrincipal().toString();
        return Long.parseLong(principal);
    }

}
