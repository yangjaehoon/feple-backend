package com.feple.feple_backend.artist.dto;

import com.feple.feple_backend.global.MusicGenre;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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
    @Size(max = 200, message = "영어 이름은 200자 이하여야 합니다.")
    private String nameEn;
    @Size(max = 500, message = "별명은 500자 이하여야 합니다.")
    private String aliases;
    @NotEmpty(message = "장르를 하나 이상 선택해주세요.")
    @Builder.Default
    private List<MusicGenre> genres = new ArrayList<>();
    private String profileImageKey;
    @Min(value = 0, message = "팔로워 수는 0 이상이어야 합니다.")
    private int followerCount;
}
