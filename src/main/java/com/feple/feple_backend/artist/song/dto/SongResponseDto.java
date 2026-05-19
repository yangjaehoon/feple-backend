package com.feple.feple_backend.artist.song.dto;

import com.feple.feple_backend.artist.song.entity.Song;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SongResponseDto {

    private Long id;
    private String title;
    private String youtubeVideoId;
    private String thumbnailUrl;
    private String youtubeUrl;

    public static SongResponseDto from(Song song) {
        return SongResponseDto.builder()
                .id(song.getId())
                .title(song.getTitle())
                .youtubeVideoId(song.getYoutubeVideoId())
                .thumbnailUrl(song.getThumbnailUrl())
                .youtubeUrl("https://music.youtube.com/watch?v=" + song.getYoutubeVideoId())
                .build();
    }
}
