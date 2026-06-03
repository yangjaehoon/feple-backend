package com.feple.feple_backend.artist.dto;

import com.feple.feple_backend.artist.entity.ArtistGenre;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtistRequestDto {
    private Long id;
    @NotBlank(message = "아티스트 이름은 필수입니다.")
    @Size(max = 200, message = "아티스트 이름은 200자 이하여야 합니다.")
    private String name;
    private String nameEn;
    private ArtistGenre genre;
    private String profileImageKey;
    @Min(value = 0, message = "팔로워 수는 0 이상이어야 합니다.")
    private int followerCount;
}
