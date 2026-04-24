package com.feple.feple_backend.artist.photo.dto;

import java.time.LocalDateTime;

public record ArtistGalleryPhotoResponseDto(
        Long photoId,
        String url,
        Long uploaderUserId,
        LocalDateTime createdAt,
        String title,
        String description,
        int likeCount,
        boolean isLiked
) {}
