package com.feple.feple_backend.controller.artist;

import com.feple.feple_backend.repository.ArtistImageLikeRepository;
import com.feple.feple_backend.service.ArtistImageLikeService;
import com.feple.feple_backend.service.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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
