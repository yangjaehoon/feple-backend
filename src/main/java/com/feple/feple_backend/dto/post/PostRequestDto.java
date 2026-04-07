package com.feple.feple_backend.dto.post;

import com.feple.feple_backend.domain.post.BoardType;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRequestDto {
    @jakarta.validation.constraints.NotBlank(message = "제목을 입력해주세요.")
    @jakarta.validation.constraints.Size(max = 100, message = "제목은 100자 이내로 입력해주세요.")
    private String title;
    @jakarta.validation.constraints.NotBlank(message = "내용을 입력해주세요.")
    @jakarta.validation.constraints.Size(max = 5000, message = "내용은 5000자 이내로 입력해주세요.")
    private String content;
    @Setter
    private BoardType boardType;

}

