package com.feple.feple_backend.post.dto;

import com.feple.feple_backend.post.entity.BoardType;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRequestDto {

    public static final int MAX_IMAGES = 10;

    @jakarta.validation.constraints.NotBlank(message = "제목을 입력해주세요.")
    @jakarta.validation.constraints.Size(max = 100, message = "제목은 100자 이내로 입력해주세요.")
    private String title;
    @jakarta.validation.constraints.NotBlank(message = "내용을 입력해주세요.")
    @jakarta.validation.constraints.Size(max = 5000, message = "내용은 5000자 이내로 입력해주세요.")
    private String content;
    @Setter
    private BoardType boardType;

    private boolean anonymous;

    @Size(max = MAX_IMAGES, message = "이미지는 최대 " + MAX_IMAGES + "장까지 첨부할 수 있습니다.")
    @Builder.Default
    private List<@Size(max = 255, message = "이미지 URL이 너무 깁니다.") String> imageUrls = List.of();

}
