package com.feple.feple_backend.artist.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistRequestDto {
    public String getName;
    public String getGenre;
    private String name;
    private String genre;
    private String profileImageUrl;
}
