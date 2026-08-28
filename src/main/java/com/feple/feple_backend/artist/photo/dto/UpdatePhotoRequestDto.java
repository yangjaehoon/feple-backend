package com.feple.feple_backend.artist.photo.dto;

import com.feple.feple_backend.global.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePhotoRequestDto(
    @NotBlank(message = ValidationMessages.TITLE_REQUIRED)
    @Size(max = 100, message = ValidationMessages.TITLE_MAX_100) String title,
    @Size(max = 500, message = ValidationMessages.DESCRIPTION_MAX_500) String description
) {}
