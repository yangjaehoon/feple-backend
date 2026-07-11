package com.feple.feple_backend.artist.dto;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.global.MusicGenre;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ArtistResponseDto {

    private Long id;
    private String name;
    private String nameEn;
    private List<String> aliases;
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
        String genreDisplay = (artist.getGenres() != null && !artist.getGenres().isEmpty())
                ? artist.getGenres().stream()
                        .map(MusicGenre::getDisplayName)
                        .collect(java.util.stream.Collectors.joining(", "))
                : null;
        return ArtistResponseDto.builder()
                .id(artist.getId())
                .name(artist.getName())
                .nameEn(artist.getNameEn())
                .aliases(List.copyOf(artist.getAliases()))
                .genre(genreDisplay)
                .profileImageUrl(imageUrl)
                .followerCount(artist.getFollowerCount())
                .songCount(songCount)
                .build();
    }
}

