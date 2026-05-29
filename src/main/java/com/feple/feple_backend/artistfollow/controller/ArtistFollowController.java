package com.feple.feple_backend.artistfollow.controller;

import com.feple.feple_backend.artistfollow.dto.FollowResponseDto;
import com.feple.feple_backend.artistfollow.dto.FollowStatusDto;
import com.feple.feple_backend.artistfollow.service.ArtistFollowService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "아티스트 팔로우", description = "아티스트 팔로우·언팔로우·상태 조회")
@RestController
@RequestMapping("/artists")
@RequiredArgsConstructor
public class ArtistFollowController {

    private final ArtistFollowService artistFollowService;

    @PostMapping("/{id}/follow")
    public FollowResponseDto follow(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return artistFollowService.follow(userId, id);
    }

    @DeleteMapping("/{id}/follow")
    public FollowResponseDto unfollow(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return artistFollowService.unfollow(userId, id);
    }

    @GetMapping("/{id}/follow")
    public FollowStatusDto followStatus(@PathVariable Long id,
                                        @AuthenticationPrincipal Long userId) {
        return artistFollowService.followStatus(userId, id);
    }
}