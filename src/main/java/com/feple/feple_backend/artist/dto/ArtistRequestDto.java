package com.feple.feple_backend.artist.dto;

import com.feple.feple_backend.artist.entity.ArtistGenre;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistRequestDto {
    private Long id;
    private String name;
    private String nameEn;
    private ArtistGenre genre;
    private String profileImageUrl;
    private int followerCount;
}
