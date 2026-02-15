package com.feple.feple_backend.artistfollow.controller;

import com.feple.feple_backend.artistfollow.dto.FollowResponseDto;
import com.feple.feple_backend.artistfollow.dto.FollowStatusDto;
import com.feple.feple_backend.artistfollow.service.ArtistFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

//    @GetMapping("/{id}/follow")
//    public FollowStatusDto isFollowed(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
//        return artistFollowService.followStatus(userId, id);
//    }

    @GetMapping("/{id}/follow")
    public FollowStatusDto followStatus(@PathVariable Long id,
                                        @AuthenticationPrincipal Long userId) {
        return artistFollowService.followStatus(userId, id);
    }
}