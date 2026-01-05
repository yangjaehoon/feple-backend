package com.feple.feple_backend.artist.dto;

import com.feple.feple_backend.artist.domain.Artist;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArtistResponseDto {

    private Long id;
    private String name;
    private String genre;
    private String profileImageUrl;
    private int likeCount;

    public static ArtistResponseDto from(Artist artist) {
        return ArtistResponseDto.builder()
                .id(artist.getId())
                .name(artist.getName())
                .genre(artist.getGenre())
                .profileImageUrl(artist.getProfileImageUrl())
                .build();
    }
}

