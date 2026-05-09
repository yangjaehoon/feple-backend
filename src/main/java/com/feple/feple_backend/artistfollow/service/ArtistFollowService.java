package com.feple.feple_backend.artistfollow.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.artistfollow.dto.FollowResponseDto;
import com.feple.feple_backend.artistfollow.dto.FollowStatusDto;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.global.EntityFinder;
import com.feple.feple_backend.global.exception.AuthenticationRequiredException;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArtistFollowService {

    private final ArtistFollowRepository artistFollowRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public boolean isFollowed(Long userId, Long artistId) {
        return artistFollowRepository.existsByUserIdAndArtistId(userId, artistId);
    }

    @Transactional(readOnly = true)
    public FollowStatusDto followStatus(Long userId, Long artistId) {
        if (userId == null) {
            throw new AuthenticationRequiredException("로그인이 필요합니다.");
        }

        boolean followed = artistFollowRepository.existsByUserIdAndArtistId(userId, artistId);
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, artistId, "아티스트");
        return new FollowStatusDto(followed, artist.getFollowerCount());
    }

    @Transactional
    public FollowResponseDto follow(Long userId, Long artistId) {
        if (userId == null) {
            throw new AuthenticationRequiredException("로그인이 필요합니다.");
        }
        User user = EntityFinder.getOrThrow(userRepository::findById, userId, "사용자");
        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, artistId, "아티스트");

        if (!artistFollowRepository.existsByUserIdAndArtistId(userId, artistId)) {
            artistFollowRepository.save(ArtistFollow.of(user, artist));
            artist.incrementFollowerCount();
        }

        return new FollowResponseDto(true, artist.getFollowerCount());
    }

    @Transactional
    public FollowResponseDto unfollow(Long userId, Long artistId) {
        if (userId == null) {
            throw new AuthenticationRequiredException("로그인이 필요합니다.");
        }

        Artist artist = EntityFinder.getOrThrow(artistRepository::findById, artistId, "아티스트");

        if (artistFollowRepository.findByUserIdAndArtistId(userId, artistId).isPresent()) {
            artistFollowRepository.deleteByUserIdAndArtistId(userId, artistId);
            artist.decrementFollowerCount();
        }

        return new FollowResponseDto(false, artist.getFollowerCount());
    }
}
