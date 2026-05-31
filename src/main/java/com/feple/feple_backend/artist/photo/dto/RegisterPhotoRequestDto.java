package com.feple.feple_backend.artist.photo.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterPhotoRequestDto (
    @NotBlank(message = "오브젝트 키는 필수입니다.") String objectKey,
    @NotBlank(message = "Content-Type은 필수입니다.") String contentType,
    @NotBlank(message = "제목은 필수입니다.") String title,
    String description
){}
