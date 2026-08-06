package com.feple.feple_backend.post.dto;

import com.feple.feple_backend.post.entity.BoardType;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.user.entity.UserRole;
import java.time.LocalDateTime;
import java.util.List;
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
    private Long userId;
    private String title;
    private String content;
    private BoardType boardType;
    private int likeCount;
    private int scrapCount;
    private int commentCount;
    private int viewCount;
    private String nickname;
    private String profileImageUrl;
    private Long artistId;
    private Long festivalId;
    private String boardDisplayName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean certified;
    private UserRole userRole;
    private boolean anonymous;
    private boolean pinned;
    private boolean blinded;
    private List<String> imageUrls;
    private List<String> tags;
    private LocalDateTime deletedAt;
    private String authorLevel;

    public static PostResponseDto from(Post post) {
        return from(post, false);
    }

    public static PostResponseDto from(Post post, boolean certified) {
        boolean anon = post.isAnonymous();
        return PostResponseDto.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .title(post.getTitle())
                .content(post.getContent())
                .boardType(post.getBoardType())
                .likeCount(post.getLikeCount())
                .scrapCount(post.getScrapCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .nickname(anon ? "익명" : post.getAuthorNickname())
                .profileImageUrl(anon ? null : post.getAuthorProfileImageUrl())
                .artistId(post.getArtistId())
                .festivalId(post.getFestivalId())
                .boardDisplayName(post.getBoardDisplayName())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .certified(certified && !anon)
                .userRole(anon ? null : post.getAuthorRole())
                .anonymous(anon)
                .pinned(post.isPinned())
                .blinded(post.isBlinded())
                .imageUrls(post.getImageKeys())
                .tags(post.getTagNames())
                .deletedAt(post.getDeletedAt())
                .authorLevel(anon ? null : post.getAuthorLevel())
                .build();
    }
}
