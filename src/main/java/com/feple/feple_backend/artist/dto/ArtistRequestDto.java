package com.feple.feple_backend.artist.dto;

import com.feple.feple_backend.artist.entity.ArtistGenre;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistRequestDto {
    private Long id;
    @NotBlank(message = "아티스트 이름은 필수입니다.")
    private String name;
    private String nameEn;
    private ArtistGenre genre;
    private String profileImageUrl;
    private int followerCount;
}
