package com.feple.feple_backend.user.controller;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.song.dto.SongRequestResponseDto;
import com.feple.feple_backend.artist.song.service.SongRequestService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.comment.dto.MyCommentResponseDto;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.user.dto.UpdateBioDto;
import com.feple.feple_backend.user.dto.UpdateNicknameDto;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.dto.UserStatsDto;
import com.feple.feple_backend.user.service.DeviceTokenService;
import com.feple.feple_backend.user.service.MyPageService;
import com.feple.feple_backend.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/check-nickname")
    public ResponseEntity<java.util.Map<String, Object>> checkNickname(
            @RequestParam @NotBlank @Size(min = 2, max = 8) String nickname,
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
        UserResponseDto updated = userService.updateNickname(id, dto.getNickname());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/profile-image")
    public ResponseEntity<UserResponseDto> updateProfileImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Long userId) {
        requireSelf(id, userId);
        return ResponseEntity.ok(userService.updateProfileImage(id, file));
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
    public ResponseEntity<UserStatsDto> getUserStats(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        requireSelf(id, userId);
        return ResponseEntity.ok(myPageService.getUserStats(id));
    }

    @GetMapping("/{id}/posts")
    public ResponseEntity<CursorPage<PostResponseDto>> getMyPosts(
            @PathVariable Long id,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Long userId) {
        requireSelf(id, userId);
        return ResponseEntity.ok(myPageService.getMyPostsPaged(id, cursor, Math.min(size, 50)));
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
        return ResponseEntity.ok(userService.updateBio(id, dto.getBio()));
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
            @AuthenticationPrincipal Long userId) {
        requireSelf(id, userId);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private void requireSelf(Long pathId, Long authenticatedId) {
        if (!pathId.equals(authenticatedId))
            throw new AccessDeniedException("본인만 접근할 수 있습니다.");
    }

    record RegisterDeviceTokenRequest(
        @NotBlank(message = "토큰이 필요합니다.") String token,
        String platform
    ) {}

    @PostMapping("/device-token")
    public ResponseEntity<Void> registerDeviceToken(
            @Valid @RequestBody RegisterDeviceTokenRequest req,
            @AuthenticationPrincipal Long userId) {
        String platform = req.platform() != null ? req.platform() : "android";
        deviceTokenService.register(userId, req.token(), platform);
        return ResponseEntity.noContent().build();
    }

    record UnregisterDeviceTokenRequest(
        @NotBlank(message = "토큰이 필요합니다.") String token
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
