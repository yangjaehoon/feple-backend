package com.feple.feple_backend.artistfollow.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.artistfollow.dto.FollowResponseDto;
import com.feple.feple_backend.artistfollow.dto.FollowStatusDto;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ArtistFollowServiceImpl implements ArtistFollowService {

    private final ArtistFollowRepository artistFollowRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowed(Long userId, Long artistId) {
        return artistFollowRepository.existsByUserIdAndArtistId(userId, artistId);
    }

    @Override
    @Transactional(readOnly = true)
    public FollowStatusDto followStatus(Long userId, Long artistId) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, artistId, "아티스트");
        boolean followed = userId != null && artistFollowRepository.existsByUserIdAndArtistId(userId, artistId);
        return new FollowStatusDto(followed, artist.getFollowerCount());
    }

    @Override
    @Transactional
    public FollowResponseDto follow(Long userId, Long artistId) {
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, artistId, "아티스트");

        // 이미 팔로우한 경우 멱등성 보장 — DataIntegrityViolationException을 @Transactional 내에서
        // catch하면 트랜잭션이 rollbackOnly로 마킹되어 UnexpectedRollbackException(500) 발생
        if (!artistFollowRepository.existsByUserIdAndArtistId(userId, artistId)) {
            artistFollowRepository.save(ArtistFollow.of(user, artist));
            artist.incrementFollowerCount();
        }

        return new FollowResponseDto(true, artistRepository.findFollowerCountById(artistId));
    }

    @Override
    @Transactional
    public FollowResponseDto unfollow(Long userId, Long artistId) {
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, artistId, "아티스트");

        int deleted = artistFollowRepository.deleteByUserIdAndArtistId(userId, artistId);
        if (deleted > 0) {
            artist.decrementFollowerCount();
        }

        return new FollowResponseDto(false, artist.getFollowerCount());
    }
}
