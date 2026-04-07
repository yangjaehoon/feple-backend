package com.feple.feple_backend.dto.comment;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateCommentDto {
    private Long postId;
    @jakarta.validation.constraints.NotBlank(message = "내용을 입력해주세요.")
    @jakarta.validation.constraints.Size(max = 1000, message = "댓글은 1000자 이내로 입력해주세요.")
    private String content;
}


