package com.feple.feple_backend.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateCommentDto {
    @NotNull(message = "게시글 ID는 필수입니다.")
    @Positive(message = "게시글 ID는 양수여야 합니다.")
    private Long postId;
    @NotBlank(message = "내용을 입력해주세요.")
    @Size(max = 1000, message = "댓글은 1000자 이내로 입력해주세요.")
    private String content;
}


