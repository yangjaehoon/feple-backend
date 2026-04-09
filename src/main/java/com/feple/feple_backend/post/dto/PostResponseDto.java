package com.feple.feple_backend.post.dto;

import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private int commentCount;
    private String nickname;
    private String profileImageUrl;
    private Long artistId;
    private Long festivalId;
    private String boardDisplayName;
    private LocalDateTime createdAt;

    public static PostResponseDto from(Post post) {
        String boardDisplayName;
        if (post.getArtist() != null) {
            boardDisplayName = post.getArtist().getName() + " 게시판";
        } else if (post.getFestival() != null) {
            boardDisplayName = post.getFestival().getTitle() + " 게시판";
        } else if (post.getBoardType() == BoardType.FREE) {
            boardDisplayName = "자유 게시판";
        } else if (post.getBoardType() == BoardType.MATE) {
            boardDisplayName = "동행 게시판";
        } else {
            boardDisplayName = "게시판";
        }

        return PostResponseDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .boardType(post.getBoardType())
                .likeCount(post.getLikeCount())
                .commentCount(post.getComments().size())
                .nickname(post.getUser().getNickname())
                .profileImageUrl(post.getUser().getProfileImageUrl())
                .artistId(post.getArtist() != null ? post.getArtist().getId() : null)
                .festivalId(post.getFestival() != null ? post.getFestival().getId() : null)
                .boardDisplayName(boardDisplayName)
                .createdAt(post.getCreatedAt())
                .build();
    }
}
