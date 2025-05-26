package com.feple.feple_backend.dto.comment;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateCommentDto {
    private Long postId;
    private Long userId;
    private String content;
}


