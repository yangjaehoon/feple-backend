package com.feple.feple_backend.artist.photo;

public record RegisterPhotoRequestDto (
    String objectKey,
    String contentType,
    String title,
    String description
){}
