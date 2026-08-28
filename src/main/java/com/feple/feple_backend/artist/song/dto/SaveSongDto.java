package com.feple.feple_backend.artist.song.dto;

import com.feple.feple_backend.global.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveSongDto {
    @NotBlank(message = "YouTube Video ID를 입력해주세요.")
    @Size(max = 20)
    private String youtubeVideoId;
    @NotBlank(message = ValidationMessages.SONG_TITLE_REQUIRED)
    @Size(max = 200)
    private String title;
    @Size(max = 500)
    private String thumbnailUrl;
}
