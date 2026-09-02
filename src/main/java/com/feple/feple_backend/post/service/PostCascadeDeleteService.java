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
}
