package com.feple.feple_backend.artist.song.dto;

import com.feple.feple_backend.artist.song.entity.Song;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SongResponseDto {

    private Long id;
    private String title;
    private String youtubeVideoId;
    private String thumbnailUrl;
    private String youtubeUrl;
    private int festivalCount;
    private List<SongFestivalDto> festivals;

    public static SongResponseDto from(Song song) {
        return SongResponseDto.builder()
                .id(song.getId())
                .title(song.getTitle())
                .youtubeVideoId(song.getYoutubeVideoId())
                .thumbnailUrl(song.getThumbnailUrl())
                .youtubeUrl("https://music.youtube.com/watch?v=" + song.getYoutubeVideoId())
                .festivalCount(0)
                .festivals(List.of())
                .build();
    }

    public static SongResponseDto from(Song song, int festivalCount, List<SongFestivalDto> festivals) {
        return SongResponseDto.builder()
                .id(song.getId())
                .title(song.getTitle())
                .youtubeVideoId(song.getYoutubeVideoId())
                .thumbnailUrl(song.getThumbnailUrl())
                .youtubeUrl("https://music.youtube.com/watch?v=" + song.getYoutubeVideoId())
                .festivalCount(festivalCount)
                .festivals(festivals)
                .build();
    }
}
