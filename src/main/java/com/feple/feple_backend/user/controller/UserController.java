package com.feple.feple_backend.user.controller;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.song.dto.SongRequestResponseDto;
import com.feple.feple_backend.artist.song.service.SongRequestService;
import com.feple.feple_backend.certification.dto.CertificationResponseDto;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.comment.dto.MyCommentResponseDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.global.PageSize;
import com.feple.feple_backend.global.ValidationMessages;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.user.NicknameValidator;
import com.feple.feple_backend.user.dto.DeleteAccountRequest;
import com.feple.feple_backend.user.dto.NicknameAvailabilityResponse;
import com.feple.feple_backend.user.dto.UpdateBioDto;
import com.feple.feple_backend.user.dto.UpdateNicknameDto;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.dto.UserStatsDto;
import com.feple.feple_backend.user.entity.DevicePlatform;
import com.feple.feple_backend.user.entity.DeviceTokenRegistration;
import com.feple.feple_backend.user.service.DeviceTokenService;
import com.feple.feple_backend.user.service.MyPageService;
import com.feple.feple_backend.user.service.UserService;
import com.feple.feple_backend.userblock.dto.BlockedUserDto;
import com.feple.feple_backend.userblock.service.UserBlockService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "사용자", description = "프로필 조회·수정, 마이페이지, 디바이스 토큰")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final MyPageService myPageService;
    private final DeviceTokenService deviceTokenService;
    private final SongRequestService songRequestService;
    private final FestivalCertificationService certificationService;
    private final UserBlockService userBlockService;

    @GetMapping("/check-nickname")
    public ResponseEntity<NicknameAvailabilityResponse> checkNickname(
            @RequestParam @NotBlank
            @Size(min = NicknameValidator.MIN_NICKNAME_LENGTH, max = NicknameValidator.MAX_NICKNAME_LENGTH)
            String nickname,
            @RequestParam(required = false) Long excludeUserId) {
        return ResponseEntity.ok(userService.checkNicknameAvailable(nickname, excludeUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNicknameDto dto,
            @AuthenticationPrincipal Long userId) {
        requireSelf(id, userId);
        userService.updateNickname(id, dto.getNickname());
        return ResponseEntity.ok(userService.getUser(id));
    }

    @PostMapping("/{id}/profile-image")
    public ResponseEntity<UserResponseDto> updateProfileImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Long userId) {
        requireSelf(id, userId);
        userService.updateProfileImage(id, file);
        return ResponseEntity.ok(userService.getUser(id));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser() {
        Long userId = userService.currentUserId();
        UserResponseDto userDto = userService.getUser(userId);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/{id}/following")
    public ResponseEntity<List<ArtistResponseDto>> getFollowedArtists(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        requireSelf(id, userId);
        return ResponseEntity.ok(myPageService.getFollowedArtists(id));
    }

    @GetMapping("/{id}/liked-festivals")
    public ResponseEntity<List<FestivalResponseDto>> getLikedFestivals(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        requireSelf(id, userId);
        return ResponseEntity.ok(myPageService.getLikedFestivals(id));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<UserStatsDto> getUserStats(@PathVariable Long id) {
        return ResponseEntity.ok(myPageService.getUserStats(id));
    }

    @GetMapping("/{id}/certifications")
    public ResponseEntity<List<CertificationResponseDto>> getUserCertifications(@PathVariable Long id) {
        return ResponseEntity.ok(certificationService.getPublicCertifications(id));
    }

    @GetMapping("/{id}/posts")
    public ResponseEntity<CursorPage<PostResponseDto>> getUserPosts(
            @PathVariable Long id,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(myPageService.getPublicPostsPaged(
                id, cursor, Math.max(1, Math.min(size, PageSize.MAX_PAGE_SIZE))));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<MyCommentResponseDto>> getMyComments(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        requireSelf(id, userId);
        return ResponseEntity.ok(myPageService.getMyComments(id));
    }

    @GetMapping("/{id}/liked-posts")
    public ResponseEntity<List<PostResponseDto>> getLikedPosts(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        requireSelf(id, userId);
        return ResponseEntity.ok(myPageService.getLikedPosts(id));
    }

    @PatchMapping("/{id}/bio")
    public ResponseEntity<UserResponseDto> updateBio(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBioDto dto,
            @AuthenticationPrincipal Long userId) {
        requireSelf(id, userId);
        userService.updateBio(id, dto.getBio());
        return ResponseEntity.ok(userService.getUser(id));
    }

    @GetMapping("/{id}/song-requests")
    public ResponseEntity<List<SongRequestResponseDto>> getMySongRequests(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        requireSelf(id, userId);
        return ResponseEntity.ok(songRequestService.getMyAllRequests(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id,
            @Valid @RequestBody DeleteAccountRequest body,
            @AuthenticationPrincipal Long userId) {
        requireSelf(id, userId);
        userService.deleteUser(id, body.reason(), body.detail());
        return ResponseEntity.noContent().build();
    }

    // ── 차단 ──

    @PostMapping("/{targetId}/block")
    public ResponseEntity<Void> blockUser(
            @PathVariable Long targetId,
            @AuthenticationPrincipal Long userId) {
        userBlockService.block(userId, targetId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{targetId}/block")
    public ResponseEntity<Void> unblockUser(
            @PathVariable Long targetId,
            @AuthenticationPrincipal Long userId) {
        userBlockService.unblock(userId, targetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/blocked")
    public ResponseEntity<List<BlockedUserDto>> getBlockedUsers(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(userBlockService.getBlockedUsers(userId));
    }

    private void requireSelf(Long pathId, Long authenticatedId) {
        if (!pathId.equals(authenticatedId))
            throw new AccessDeniedException("본인만 접근할 수 있습니다.");
    }

    record RegisterDeviceTokenRequest(
        @NotBlank(message = ValidationMessages.TOKEN_REQUIRED) String token,
        String platform,
        String language
    ) {}

    @PostMapping("/device-token")
    public ResponseEntity<Void> registerDeviceToken(
            @Valid @RequestBody RegisterDeviceTokenRequest req,
            @AuthenticationPrincipal Long userId) {
        String platform = req.platform() != null ? req.platform() : DevicePlatform.DEFAULT;
        // 언어 기본값("ko")은 UserDeviceToken이 유일한 출처 — 여기서는 원본 값을 그대로 넘긴다.
        deviceTokenService.register(userId, new DeviceTokenRegistration(req.token(), platform, req.language()));
        return ResponseEntity.noContent().build();
    }

    record UnregisterDeviceTokenRequest(
        @NotBlank(message = ValidationMessages.TOKEN_REQUIRED) String token
    ) {}

    /** FCM 디바이스 토큰 삭제 (로그아웃 시) */
    @DeleteMapping("/device-token")
    public ResponseEntity<Void> unregisterDeviceToken(
            @Valid @RequestBody UnregisterDeviceTokenRequest req,
            @AuthenticationPrincipal Long userId) {
        deviceTokenService.unregister(userId, req.token());
        return ResponseEntity.noContent().build();
    }

}
