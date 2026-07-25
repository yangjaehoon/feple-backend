package com.feple.feple_backend.festival.suggestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubmitFestivalSuggestionDto {
    @NotBlank(message = "페스티벌 이름을 입력해주세요.")
    @Size(max = 100, message = "페스티벌 이름은 100자 이내로 입력해주세요.")
    private String festivalName;

    @Size(max = 255, message = "메모는 255자 이내로 입력해주세요.")
    private String note;
}
