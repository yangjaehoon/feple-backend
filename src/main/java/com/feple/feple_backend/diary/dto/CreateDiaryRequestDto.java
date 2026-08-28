package com.feple.feple_backend.diary.dto;

import com.feple.feple_backend.diary.entity.DiaryVisibility;
import com.feple.feple_backend.global.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateDiaryRequestDto(
        @NotNull(message = ValidationMessages.FESTIVAL_ID_REQUIRED) Long festivalId,
        @NotBlank(message = ValidationMessages.CONTENT_REQUIRED)
        @Size(max = 2000, message = ValidationMessages.DIARY_CONTENT_MAX_2000) String content,
        @NotNull(message = ValidationMessages.VISIBILITY_REQUIRED) DiaryVisibility visibility,
        @Size(max = 5, message = "사진은 최대 5장까지 첨부할 수 있습니다.") List<@NotBlank String> photoKeys
) {}
