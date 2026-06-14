package com.feple.feple_backend.artist.dto;

import com.feple.feple_backend.artist.entity.Artist;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArtistResponseDto {

    private Long id;
    private String name;
    private String nameEn;
    private String genre;
    private String profileImageUrl;
    private int followerCount;
    private int songCount;

    public static ArtistResponseDto from(Artist artist) {
        return from(artist, artist.getProfileImageKey(), 0);
    }

    public static ArtistResponseDto from(Artist artist, String imageUrl) {
        return from(artist, imageUrl, 0);
    }

    public static ArtistResponseDto from(Artist artist, String imageUrl, int songCount) {
        return ArtistResponseDto.builder()
                .id(artist.getId())
                .name(artist.getName())
                .nameEn(artist.getNameEn())
                .genre(artist.getGenre() != null ? artist.getGenre().getDisplayName() : null)
                .profileImageUrl(imageUrl)
                .followerCount(artist.getFollowerCount())
                .songCount(songCount)
                .build();
    }
}

