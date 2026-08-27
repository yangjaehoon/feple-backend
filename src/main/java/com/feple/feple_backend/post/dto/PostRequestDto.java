package com.feple.feple_backend.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRequestDto {

    public static final int MAX_IMAGES = 10;
    public static final int MAX_TAGS = 5;

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 100, message = "제목은 100자 이내로 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    @Size(max = 5000, message = "내용은 5000자 이내로 입력해주세요.")
    private String content;

    private boolean anonymous;

    @Size(max = MAX_IMAGES, message = "이미지는 최대 " + MAX_IMAGES + "장까지 첨부할 수 있습니다.")
    @Builder.Default
    private List<@Size(max = 255, message = "이미지 URL이 너무 깁니다.") String> imageUrls = List.of();

    @Size(max = MAX_TAGS, message = "태그는 최대 " + MAX_TAGS + "개까지 입력할 수 있습니다.")
    @Builder.Default
    private List<@Size(max = 30, message = "태그는 30자 이내로 입력해주세요.") String> tags = List.of();
}
