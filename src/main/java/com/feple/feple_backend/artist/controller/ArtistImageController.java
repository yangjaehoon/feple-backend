package com.feple.feple_backend.artist.controller;

import com.feple.feple_backend.service.ArtistImageLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/artist images")
public class ArtistImageController {
    private final ArtistImageLikeService likeService;

    @PostMapping("/{id}/like")
    public void likeImage(@PathVariable Long id, @RequestParam Long userId) {
        likeService.likeImage(id, userId);
    }

    @DeleteMapping("/{id}/like")
    public void unlikeImage(@PathVariable Long id, @RequestParam Long userId) {
        likeService.unlikeImage(id, userId);
    }

}
