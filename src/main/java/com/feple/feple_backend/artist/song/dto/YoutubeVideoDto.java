package com.feple.feple_backend.artist.song.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class YoutubeVideoDto {
    private String videoId;
    private String title;
    private String channelTitle;
    private String thumbnailUrl;
}
