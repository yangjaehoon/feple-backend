package com.feple.feple_backend.artist.song.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveSongRequestDto {
    private String youtubeVideoId;
    private String title;
    private String thumbnailUrl;
}
