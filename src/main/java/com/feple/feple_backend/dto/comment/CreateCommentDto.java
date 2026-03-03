package com.feple.feple_backend.dto.comment;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateCommentDto {
    private Long postId;
    @jakarta.validation.constraints.NotBlank(message = "내용을 입력해주세요.")
    private String content;
}


