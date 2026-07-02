package com.feple.feple_backend.artist.photo.dto;

import com.feple.feple_backend.artist.photo.entity.ArtistGalleryPhoto;

import java.time.LocalDateTime;

public record ArtistGalleryPhotoResponseDto(
        Long photoId,
        String url,
        Long uploaderUserId,
        String uploaderNickname,
        LocalDateTime createdAt,
        String title,
        String description,
        int likeCount,
        boolean isLiked,
        boolean isAnonymous
) {
    private static final String ANONYMOUS_LABEL = "익명";

    /**
     * @param currentUserId 현재 요청자 ID — 본인 글은 익명이어도 uploaderUserId를 그대로 반환해 수정/삭제 가능하게 함
     */
    public static ArtistGalleryPhotoResponseDto from(ArtistGalleryPhoto photo, String url, boolean isLiked, Long currentUserId) {
        boolean isOwner = currentUserId != null && currentUserId.equals(photo.getUploaderId());
        Long exposedUploaderId = (photo.isAnonymous() && !isOwner) ? null : photo.getUploaderId();
        String nickname = photo.isAnonymous() ? ANONYMOUS_LABEL : photo.getUploaderNickname();
        return new ArtistGalleryPhotoResponseDto(
                photo.getId(),
                url,
                exposedUploaderId,
                nickname,
                photo.getCreatedAt(),
                photo.getTitle(),
                photo.getDescription(),
                photo.getLikeCount(),
                isLiked,
                photo.isAnonymous()
        );
    }
}
