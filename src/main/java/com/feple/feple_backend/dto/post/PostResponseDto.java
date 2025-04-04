package com.feple.feple_backend.dto.post;

import com.feple.feple_backend.domain.post.BoardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostResponseDto {
    private Long id;
    private String title;
    private String content;
    private BoardType boardType;
    private int likeCount;
}
