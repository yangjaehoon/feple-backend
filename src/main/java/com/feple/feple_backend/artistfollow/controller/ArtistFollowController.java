package com.feple.feple_backend.artistfollow.controller;

import com.feple.feple_backend.artistfollow.service.ArtistFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/artists")
@RequiredArgsConstructor
public class ArtistFollowController {

    private final ArtistFollowService artistFollowService;

    @PostMapping("/{id}/follow")
    public void follow(@PathVariable Long id, /* currentUserId */ Long userId) {
        artistFollowService.follow(userId, id);
    }

    @DeleteMapping("/{id}/follow")
    public void unfollow(@PathVariable Long id, /* currentUserId */ Long userId) {
        artistFollowService.unfollow(userId, id);
    }
}