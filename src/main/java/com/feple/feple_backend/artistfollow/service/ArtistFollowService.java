package com.feple.feple_backend.artistfollow.service;

import com.feple.feple_backend.artistfollow.dto.FollowResponseDto;
import com.feple.feple_backend.artistfollow.dto.FollowStatusDto;

import java.util.List;

public interface ArtistFollowService {

    /** 해당 아티스트를 팔로우 중인 유저 ID 목록 */
    List<Long> getFollowerUserIds(Long artistId);

    FollowStatusDto followStatus(Long userId, Long artistId);

    FollowResponseDto follow(Long userId, Long artistId);

    FollowResponseDto unfollow(Long userId, Long artistId);

    /** 회원 탈퇴 시 해당 유저의 팔로우 데이터 일괄 제거 */
    void removeAllByUser(Long userId);

    /** 아티스트 삭제 시 해당 아티스트의 팔로우 데이터 일괄 제거 */
    void removeAllByArtist(Long artistId);
}
