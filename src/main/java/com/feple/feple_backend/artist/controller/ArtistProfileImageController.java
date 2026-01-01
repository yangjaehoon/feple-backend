package com.feple.feple_backend.artist.controller;

import com.feple.feple_backend.service.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/artist-profile-image")
public class ArtistProfileImageController {

    private final S3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadArtistImage(@RequestParam("file") MultipartFile file) throws IOException {
        String imageUrl = s3Service.upload(file, "artist-profile-images");
        return ResponseEntity.ok(imageUrl);
    }

}
