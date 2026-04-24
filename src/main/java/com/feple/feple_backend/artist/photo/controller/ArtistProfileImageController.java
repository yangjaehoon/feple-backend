package com.feple.feple_backend.artist.photo.controller;

import com.feple.feple_backend.artist.photo.service.ArtistProfileImageLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/artist-image")
public class ArtistProfileImageController {
    private final ArtistProfileImageLikeService likeService;

    @PostMapping("/{id}/like")
    public void likeImage(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        likeService.likeImage(id, userId);
    }

    @DeleteMapping("/{id}/like")
    public void unlikeImage(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        likeService.unlikeImage(id, userId);
    }

}
