package com.feple.feple_backend.post.service;

public interface PostCascadeDeleteService {
    /** 회원 탈퇴 시 해당 유저의 게시글 좋아요/스크랩 데이터 일괄 제거 */
    void removePostActivityByUser(Long userId);
}
