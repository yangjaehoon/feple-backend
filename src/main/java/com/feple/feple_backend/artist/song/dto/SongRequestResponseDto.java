package com.feple.feple_backend.artist.song.dto;

import com.feple.feple_backend.artist.song.entity.SongRequest;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class SongRequestResponseDto {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private Long id;
    private String songTitle;
    private String youtubeUrl;
    private String status;
    private String createdAt;
    private Long userId;
    private String userNickname;
    private Long artistId;
    private String artistName;

    public static SongRequestResponseDto from(SongRequest request, String userNickname) {
        return SongRequestResponseDto.builder()
                .id(request.getId())
                .songTitle(request.getSongTitle())
                .youtubeUrl(request.getYoutubeUrl())
                .status(request.getStatus().name())
                .createdAt(request.getCreatedAt() != null
                        ? request.getCreatedAt().format(FORMATTER)
                        : null)
                .userId(request.getUserId())
                .userNickname(userNickname)
                .artistId(request.getArtistId())
                .artistName(request.getArtistName())
                .build();
    }
}
