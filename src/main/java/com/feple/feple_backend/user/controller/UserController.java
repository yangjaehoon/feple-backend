package com.feple.feple_backend.user.controller;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.dto.comment.MyCommentResponseDto;
import com.feple.feple_backend.dto.post.PostResponseDto;
import com.feple.feple_backend.user.dto.UpdateNicknameDto;
import com.feple.feple_backend.user.dto.OAuthUserInfo;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.dto.UserStatsDto;
import com.feple.feple_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/check-nickname")
    public ResponseEntity<java.util.Map<String, Object>> checkNickname(
            @RequestParam String nickname,
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
            @RequestBody UpdateNicknameDto dto,
            @AuthenticationPrincipal Long userId) {
        if (!id.equals(userId)) throw new AccessDeniedException("본인 정보만 수정할 수 있습니다.");
        UserResponseDto updated = userService.updateNickname(id, dto.getNickname());
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/profile-image")
    public ResponseEntity<UserResponseDto> updateProfileImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Long userId) throws java.io.IOException {
        if (!id.equals(userId)) throw new AccessDeniedException("본인 정보만 수정할 수 있습니다.");
        return ResponseEntity.ok(userService.updateProfileImage(id, file));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(Authentication auth) {
        Long userId = userService.currentUserId();
        UserResponseDto userDto = userService.getUser(userId);
        return ResponseEntity.ok(userDto);
    }

    @GetMapping("/{id}/following")
    public ResponseEntity<List<ArtistResponseDto>> getFollowedArtists(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getFollowedArtists(id));
    }

    @GetMapping("/{id}/liked-festivals")
    public ResponseEntity<List<FestivalResponseDto>> getLikedFestivals(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        if (!id.equals(userId)) throw new AccessDeniedException("본인의 정보만 조회할 수 있습니다.");
        return ResponseEntity.ok(userService.getLikedFestivals(id));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<UserStatsDto> getUserStats(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserStats(id));
    }

    @GetMapping("/{id}/posts")
    public ResponseEntity<List<PostResponseDto>> getMyPosts(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        if (!id.equals(userId)) throw new AccessDeniedException("본인의 정보만 조회할 수 있습니다.");
        return ResponseEntity.ok(userService.getMyPosts(id));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<MyCommentResponseDto>> getMyComments(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        if (!id.equals(userId)) throw new AccessDeniedException("본인의 정보만 조회할 수 있습니다.");
        return ResponseEntity.ok(userService.getMyComments(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id,
                                           @AuthenticationPrincipal Long userId) {
        if (!id.equals(userId)) throw new AccessDeniedException("본인 계정만 삭제할 수 있습니다.");
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}
