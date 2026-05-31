package com.feple.feple_backend.artist.song.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubmitSongRequestDto {
    private String songTitle;
    private String youtubeUrl;
}
