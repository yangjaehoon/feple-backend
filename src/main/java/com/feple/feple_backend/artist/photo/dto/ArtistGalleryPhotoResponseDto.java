package com.feple.feple_backend.artist.photo.dto;

import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhoto;

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
) {
    public static ArtistGalleryPhotoResponseDto from(ArtistGalleryPhoto photo, String url, boolean isLiked) {
        return new ArtistGalleryPhotoResponseDto(
                photo.getId(),
                url,
                photo.getUploaderId(),
                photo.getCreatedAt(),
                photo.getTitle(),
                photo.getDescription(),
                photo.getLikeCount(),
                isLiked
        );
    }
}
