package com.feple.feple_backend.artist.photo;

import java.time.LocalDateTime;

public record ArtistPhotoResponseDto(
        Long photoId,
        String url,
        Long uploaderUserId,
        LocalDateTime createdAt,
        String title,
        String description,
        int likecount,
        boolean isLiked
) {}