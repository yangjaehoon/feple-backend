package com.feple.feple_backend.artist.dto;

import com.feple.feple_backend.artist.entity.Artist;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArtistResponseDto {

    private Long id;
    private String name;
    private String genre;
    private String profileImageUrl;
    private int followerCount;

    public static ArtistResponseDto from(Artist artist) {
        return from(artist, artist.getProfileImageKey());
    }

    public static ArtistResponseDto from(Artist artist, String imageUrl) {
        return ArtistResponseDto.builder()
                .id(artist.getId())
                .name(artist.getName())
                .genre(artist.getGenre() != null ? artist.getGenre().getDisplayName() : null)
                .profileImageUrl(imageUrl)
                .followerCount(artist.getFollowerCount())
                .build();
    }
}

