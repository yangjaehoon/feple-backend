package com.feple.feple_backend.artistfollow.service;

import com.feple.feple_backend.artistfollow.dto.FollowResponseDto;
import com.feple.feple_backend.artistfollow.dto.FollowStatusDto;

public interface ArtistFollowService {

    boolean isFollowed(Long userId, Long artistId);

    FollowStatusDto followStatus(Long userId, Long artistId);

    FollowResponseDto follow(Long userId, Long artistId);

    FollowResponseDto unfollow(Long userId, Long artistId);
}
