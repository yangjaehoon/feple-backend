package com.feple.feple_backend.artist.service;

import com.feple.feple_backend.artist.domain.ArtistPhoto;
import com.feple.feple_backend.artist.dto.ArtistPhotoResponseDto;
import com.feple.feple_backend.artist.repository.ArtistPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ArtistPhotoService {

    private final ArtistPhotoRepository artistPhotoRepository;
    private final S3PresignService s3PresignService;

    public ArtistPhotoResponseDto register(
            Long artistId,
            String objectKey,
            String contentType,
            String title,
            String description) {
        String prefix = "artist-photos/" + artistId + "/";
        if (objectKey == null || !objectKey.startsWith(prefix)) {
            throw new IllegalArgumentException("Invalid objectKey");
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = (Long) principal;

        ArtistPhoto saved = artistPhotoRepository.save(
                new ArtistPhoto(artistId, userId, objectKey, contentType, title, description)
        );

        String url = s3PresignService.presignGetUrl(saved.getS3Key());
        return new ArtistPhotoResponseDto(
                saved.getId(),
                url,
                saved.getUploaderUserId(),
                saved.getCreatedAt(),
                saved.getTitle(),
                saved.getDescription());
    }

    public List<ArtistPhotoResponseDto> list(Long artistId) {
        return artistPhotoRepository.findByArtistIdOrderByIdDesc(artistId)
                .stream()
                .map(p -> new ArtistPhotoResponseDto(
                        p.getId(),
                        s3PresignService.presignGetUrl(p.getS3Key()),
                        p.getUploaderUserId(),
                        p.getCreatedAt(),
                        p.getTitle(),
                        p.getDescription()
                ))
                .toList();
    }
}