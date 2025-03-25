package com.feple.feple_backend.dto.artist;

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
}

