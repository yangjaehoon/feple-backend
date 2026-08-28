package com.feple.feple_backend.post.dto;

import com.feple.feple_backend.global.ValidationMessages;
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
    @Size(max = 100, message = ValidationMessages.TITLE_MAX_100)
    private String title;

    @NotBlank(message = ValidationMessages.CONTENT_BLANK)
    @Size(max = 5000, message = ValidationMessages.POST_CONTENT_MAX_5000)
    private String content;

    private boolean anonymous;

    @Size(max = MAX_IMAGES, message = "이미지는 최대 " + MAX_IMAGES + "장까지 첨부할 수 있습니다.")
    @Builder.Default
    private List<@Size(max = 255, message = ValidationMessages.IMAGE_URL_TOO_LONG) String> imageUrls = List.of();

    @Size(max = MAX_TAGS, message = "태그는 최대 " + MAX_TAGS + "개까지 입력할 수 있습니다.")
    @Builder.Default
    private List<@Size(max = 30, message = "태그는 30자 이내로 입력해주세요.") String> tags = List.of();
}
