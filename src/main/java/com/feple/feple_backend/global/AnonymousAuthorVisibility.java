package com.feple.feple_backend.global;

/**
 * 익명 게시글/댓글의 작성자 id·인증뱃지 노출 여부 판정. PostResponseDto/CommentResponseDto가 동일하게
 * "익명이면 가리되, 관리자 화면이거나 조회자 본인 글이면 예외적으로 노출"하는 규칙을 공유한다.
 */
public final class AnonymousAuthorVisibility {

    private AnonymousAuthorVisibility() {}

    public record Fields(Long authorId, boolean certified) {}

    /** 호출부(뷰어)에 따라 달라지는 조건들을 묶어 resolve()의 파라미터를 3개 이하로 유지한다. */
    public record Request(boolean certified, Long viewerId, boolean revealAuthorId) {}

    public static Fields resolve(boolean anonymous, Long authorId, Request request) {
        if (!anonymous) return new Fields(authorId, request.certified());
        boolean exposeAuthorId = request.revealAuthorId() || OwnershipValidator.isOwner(authorId, request.viewerId());
        return new Fields(exposeAuthorId ? authorId : null, false);
    }
}
