package com.feple.feple_backend.artistfollow.service;

import com.feple.feple_backend.artistfollow.dto.FollowResponseDto;
import com.feple.feple_backend.artistfollow.dto.FollowStatusDto;

public interface ArtistFollowService {

    boolean isFollowed(Long userId, Long artistId);

    FollowStatusDto followStatus(Long userId, Long artistId);

    FollowResponseDto follow(Long userId, Long artistId);

    FollowResponseDto unfollow(Long userId, Long artistId);

    /** 회원 탈퇴 시 해당 유저의 팔로우 데이터 일괄 제거 */
    void removeAllByUser(Long userId);
}
