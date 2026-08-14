package com.feple.feple_backend.diary.dto;

import com.feple.feple_backend.diary.entity.DiaryVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateDiaryRequestDto(
        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 2000, message = "내용은 2000자 이내로 작성해주세요.") String content,
        @NotNull(message = "공개 범위는 필수입니다.") DiaryVisibility visibility
) {}
