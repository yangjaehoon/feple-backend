package com.feple.feple_backend.artist.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistRequestDto {
    private Long id;
    private String name;
    private String genre;
    private String profileImageUrl;
    private int followerCount;
}
