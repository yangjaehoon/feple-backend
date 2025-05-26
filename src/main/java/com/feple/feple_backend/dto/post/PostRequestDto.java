package com.feple.feple_backend.dto.post;

import com.feple.feple_backend.domain.post.BoardType;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostRequestDto {
    private Long userId;
    private String title;
    private String content;
    @Setter
    private BoardType boardType;

}

