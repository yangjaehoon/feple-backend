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
    private Long artistId;
    private Long festivalId;

    public static PostResponseDto from(Post post) {
        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .boardType(post.getBoardType())
                .likeCount(post.getLikeCount())
                .nickname(post.getUser().getNickname())  // Lazy 로딩 주의
                .artistId(post.getArtist() != null ? post.getArtist().getId() : null)
                .festivalId(post.getFestival() != null ? post.getFestival().getId() : null)
                .build();
    }
}
