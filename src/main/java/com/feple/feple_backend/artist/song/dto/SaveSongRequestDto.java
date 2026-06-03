package com.feple.feple_backend.artist.song.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveSongRequestDto {
    @NotBlank(message = "YouTube Video ID를 입력해주세요.")
    @Size(max = 20)
    private String youtubeVideoId;
    @NotBlank(message = "곡 제목을 입력해주세요.")
    @Size(max = 200)
    private String title;
    @Size(max = 500)
    private String thumbnailUrl;
}
