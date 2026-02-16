package com.feple.feple_backend.artist.dto;

public record RegisterPhotoRequestDto (
    String objectKey,
    String contentType,
    String title,
    String description
){}
