package com.feple.feple_backend.artist.dto;

import java.time.LocalDateTime;

public record ArtistPhotoResponseDto(
        Long photoId,
        String url,
        Long uploaderUserId,
        LocalDateTime createdAt,
        String title,
        String description
) {}