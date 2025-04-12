package com.feple.feple_backend.dto.post;

import com.feple.feple_backend.domain.post.BoardType;
import com.feple.feple_backend.domain.post.Post;
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
    private String nickname;

    public static PostResponseDto from(Post post) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .boardType(post.getBoardType())
                .likeCount(post.getLikeCount())
                .nickname(post.getUser().getNickname())  // Lazy 로딩 주의
                .build();
    }
}
