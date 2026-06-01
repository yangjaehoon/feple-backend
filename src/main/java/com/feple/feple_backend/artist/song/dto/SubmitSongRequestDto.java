package com.feple.feple_backend.artist.song.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubmitSongRequestDto {
    @NotBlank(message = "곡 제목을 입력해주세요.")
    @Size(max = 200, message = "곡 제목은 200자 이내로 입력해주세요.")
    private String songTitle;

    @Size(max = 500, message = "유튜브 URL은 500자 이내로 입력해주세요.")
    private String youtubeUrl;
}
