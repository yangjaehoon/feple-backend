package com.feple.feple_backend.artist.photo.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePhotoRequestDto(
    @NotBlank(message = "제목은 필수입니다.") String title,
    String description
) {}
