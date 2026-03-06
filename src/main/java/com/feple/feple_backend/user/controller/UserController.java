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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<Long> createUser(@RequestBody OAuthUserInfo dto) {
        return ResponseEntity.ok(userService.createUser(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateNicknameDto dto) {
        UserResponseDto updated = userService.updateNickname(id, dto.getNickname());
        return ResponseEntity.ok(updated);
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
    public ResponseEntity<List<FestivalResponseDto>> getLikedFestivals(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getLikedFestivals(id));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<UserStatsDto> getUserStats(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserStats(id));
    }

    @GetMapping("/{id}/posts")
    public ResponseEntity<List<PostResponseDto>> getMyPosts(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getMyPosts(id));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<List<MyCommentResponseDto>> getMyComments(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getMyComments(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

}
