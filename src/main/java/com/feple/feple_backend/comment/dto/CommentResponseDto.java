package com.feple.feple_backend.comment.dto;

import com.feple.feple_backend.comment.entity.Comment;
import com.feple.feple_backend.file.service.FileStorageService;
import com.feple.feple_backend.global.AnonymousAuthorVisibility;
import com.feple.feple_backend.user.entity.UserRole;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponseDto {
    private Long id;
    private Long postId;
    private Long userId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean certified;
    private UserRole userRole;
    private Long parentId;
    private int likeCount;
    private boolean liked;
    private String profileImageUrl;
    private boolean anonymous;
    private boolean blinded;
    private boolean edited;
    private Long mentionedUserId;
    private String mentionedNickname;

    /**
     * 조회자 관점에 따라 달라지는 값(본인 인증 뱃지 표시 여부, 좋아요 여부, 조회자 id)을 묶어
     * from()의 파라미터를 3개 이하로 유지한다.
     *
     * @param viewerId 현재 조회자 id. 익명 댓글이라도 본인 댓글이면 userId를 그대로 노출해야
     *                 프론트가 수정/삭제 버튼을 판단할 수 있다.
     */
    public record ViewerContext(boolean certified, boolean liked, Long viewerId) {}

    public static CommentResponseDto from(Comment comment, ViewerContext viewer, FileStorageService fileStorageService) {
        return build(comment, viewer, false, fileStorageService);
    }

    /**
     * 관리자 화면 전용. 익명 댓글도 닉네임·프로필 등은 일반 사용자와 동일하게 "익명"으로 가리지만,
     * 모더레이션을 위해 작성자 userId는 가리지 않는다 — admin/post 상세 템플릿이 이 userId로
     * 작성자 상세 페이지 링크를 만든다.
     */
    public static CommentResponseDto fromForAdmin(Comment comment, ViewerContext viewer, FileStorageService fileStorageService) {
        return build(comment, viewer, true, fileStorageService);
    }

    // revealAuthorId는 실제 업무 데이터, fileStorageService는 프로필 이미지 URL을 해석해주는
    // 인프라 협력자라 파라미터 개수 규칙에서 제외한다.
    private static CommentResponseDto build(
            Comment comment, ViewerContext viewer, boolean revealAuthorId, FileStorageService fileStorageService) {
        boolean anon = comment.isAnonymous();
        AnonymousAuthorVisibility.Fields visible = AnonymousAuthorVisibility.resolve(anon, comment.getUserId(),
                new AnonymousAuthorVisibility.Request(viewer.certified(), viewer.viewerId(), revealAuthorId));
        return CommentResponseDto.builder()
                .id(comment.getId())
                .postId(comment.getPostId())
                .userId(visible.authorId())
                .nickname(anon ? AnonymousAuthorVisibility.ANONYMOUS_NICKNAME : comment.getUserNickname())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .certified(visible.certified())
                .userRole(anon ? null : comment.getUserRole())
                .parentId(comment.getParentId())
                .likeCount(comment.getLikeCount())
                .liked(viewer.liked())
                .profileImageUrl(anon ? null : fileStorageService.resolveProfileImageUrl(comment.getUserProfileImageUrl()))
                .anonymous(anon)
                .blinded(comment.isBlinded())
                .edited(comment.isEdited())
                .mentionedUserId(comment.getMentionedUserId())
                .mentionedNickname(comment.getMentionedNickname())
                .build();
    }
}
