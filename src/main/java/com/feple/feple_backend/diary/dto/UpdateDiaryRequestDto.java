package com.feple.feple_backend.diary.dto;

import com.feple.feple_backend.diary.entity.DiaryVisibility;
import com.feple.feple_backend.global.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDiaryRequestDto(
        @NotBlank(message = ValidationMessages.CONTENT_REQUIRED)
        @Size(max = 2000, message = ValidationMessages.DIARY_CONTENT_MAX_2000) String content,
        @NotNull(message = ValidationMessages.VISIBILITY_REQUIRED) DiaryVisibility visibility
) {}
