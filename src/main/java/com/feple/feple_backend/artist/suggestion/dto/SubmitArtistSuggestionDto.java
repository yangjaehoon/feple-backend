package com.feple.feple_backend.artist.suggestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubmitArtistSuggestionDto {
    @NotBlank(message = "아티스트 이름을 입력해주세요.")
    @Size(max = 100, message = "아티스트 이름은 100자 이내로 입력해주세요.")
    private String artistName;

    @Size(max = 500, message = "메모는 500자 이내로 입력해주세요.")
    private String note;
}
