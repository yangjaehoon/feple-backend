package com.feple.feple_backend.diary.dto;

import com.feple.feple_backend.diary.entity.DiaryVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateDiaryRequestDto(
        @NotNull(message = "페스티벌 ID는 필수입니다.") Long festivalId,
        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 2000, message = "내용은 2000자 이내로 작성해주세요.") String content,
        @NotNull(message = "공개 범위는 필수입니다.") DiaryVisibility visibility,
        @Size(max = 5, message = "사진은 최대 5장까지 첨부할 수 있습니다.") List<@NotBlank String> photoKeys
) {}
