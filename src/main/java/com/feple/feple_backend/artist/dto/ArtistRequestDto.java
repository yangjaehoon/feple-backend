package com.feple.feple_backend.artist.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArtistRequestDto {
    private String name;
    private String genre;
    private String profileImageUrl;
}
