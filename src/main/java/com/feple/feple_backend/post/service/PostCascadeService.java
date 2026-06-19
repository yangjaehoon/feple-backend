package com.feple.feple_backend.post.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.festival.entity.Festival;

public interface PostCascadeService {
    void deletePostsByFestival(Festival festival);
    void deletePostsByArtist(Artist artist);
    /** 회원 탈퇴 시 해당 유저의 게시글 좋아요/스크랩 데이터 일괄 제거 */
    void removePostActivityByUser(Long userId);
}
