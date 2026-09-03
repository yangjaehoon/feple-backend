package com.feple.feple_backend.post.service;

public interface PostCascadeDeleteService {
    /** 회원 탈퇴 시 해당 유저의 게시글 좋아요/스크랩 데이터 일괄 제거 */
    void removePostActivityByUser(Long userId);

    /**
     * 회원 완전 삭제(hardDelete) 시 users 행 물리 삭제 전에 정리해야 하는 게시글 도메인 잔여 참조
     * — 임시저장(post_draft), 이 유저가 낸 게시글 신고(post_report.reporter_id). 둘 다 users FK RESTRICT.
     * (일반 탈퇴는 신고 이력을 보존하므로 이 메서드를 호출하지 않는다.)
     */
    void removeAuthoredArtifactsByUser(Long userId);

    /**
     * 회원 완전 삭제(hardDelete) 시 이 유저가 <b>작성한 게시글</b>과 그에 딸린 모든 것(다른 유저가 단
     * 댓글·좋아요·스크랩·신고, 태그·이미지, 관련 알림)을 물리 삭제한다. 소프트 삭제·블라인드된 글도
     * 포함한다. 되돌릴 수 없으며, 일반 탈퇴(익명화)는 이 메서드를 호출하지 않는다.
     */
    void purgeAuthoredPostsByUser(Long userId);
}
