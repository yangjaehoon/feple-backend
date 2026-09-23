package com.feple.feple_backend.post.dto;

import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.AnonymousAuthorVisibility;
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
@Builder(toBuilder = true)
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

    /**
     * 조회자 관점에 따라 달라지는 값(본인 인증 뱃지 표시 여부, 조회자 id)을 묶어
     * from()의 파라미터를 3개 이하로 유지한다.
     *
     * @param viewerId 현재 조회자 id. 익명 글이라도 본인 글이면 userId를 그대로 노출해야
     *                 프론트가 수정/삭제 버튼을 판단할 수 있다 — nickname/profileImageUrl 등
     *                 "타인에게 안 보여줄" 필드와 달리 userId는 본인에게는 예외적으로 필요하다.
     */
    public record ViewerContext(boolean certified, Long viewerId) {}

    public static PostResponseDto from(Post post, FileStorageService fileStorageService) {
        return from(post, new ViewerContext(false, null), fileStorageService);
    }

    public static PostResponseDto from(Post post, boolean certified, FileStorageService fileStorageService) {
        return from(post, new ViewerContext(certified, null), fileStorageService);
    }

    public static PostResponseDto from(Post post, boolean certified, Long viewerId, FileStorageService fileStorageService) {
        return from(post, new ViewerContext(certified, viewerId), fileStorageService);
    }

    public static PostResponseDto from(Post post, ViewerContext viewer, FileStorageService fileStorageService) {
        return build(post, viewer, false, fileStorageService);
    }

    /**
     * 관리자 화면 전용. 익명 글도 닉네임·프로필 등은 일반 사용자와 동일하게 "익명"으로 가리지만,
     * 모더레이션을 위해 작성자 userId는 가리지 않는다 — admin/post 템플릿이 이 userId로
     * 작성자 상세 페이지 링크를 만든다.
     */
    public static PostResponseDto fromForAdmin(Post post, FileStorageService fileStorageService) {
        return build(post, new ViewerContext(false, null), true, fileStorageService);
    }

    // revealAuthorId는 실제 업무 데이터, fileStorageService는 프로필 이미지 URL을 해석해주는
    // 인프라 협력자라 파라미터 개수 규칙에서 제외한다.
    private static PostResponseDto build(Post post, ViewerContext viewer, boolean revealAuthorId, FileStorageService fileStorageService) {
        boolean anon = post.isAnonymous();
        AnonymousAuthorVisibility.Fields visible = AnonymousAuthorVisibility.resolve(anon, post.getUserId(),
                new AnonymousAuthorVisibility.Request(viewer.certified(), viewer.viewerId(), revealAuthorId));
        return PostResponseDto.builder()
                .id(post.getId())
                .userId(visible.authorId())
                .title(post.getTitle())
                .content(post.getContent())
                .boardType(post.getBoardType())
                .likeCount(post.getLikeCount())
                .scrapCount(post.getScrapCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .nickname(anon ? AnonymousAuthorVisibility.ANONYMOUS_NICKNAME : post.getAuthorNickname())
                .profileImageUrl(anon ? null : fileStorageService.resolveProfileImageUrl(post.getAuthorProfileImageUrl()))
                .artistId(post.getArtistId())
                .festivalId(post.getFestivalId())
                .boardDisplayName(post.getBoardDisplayName())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .certified(visible.certified())
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
