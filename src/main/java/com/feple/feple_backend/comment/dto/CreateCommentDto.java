package com.feple.feple_backend.comment.dto;

import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.global.ValidationMessages;
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
    @NotBlank(message = ValidationMessages.CONTENT_BLANK)
    @Size(max = Comment.MAX_CONTENT_LENGTH, message = ValidationMessages.COMMENT_MAX_1000)
    private String content;
    private Long parentId; // 대댓글인 경우 부모 댓글 ID
    private boolean anonymous = false;
}
