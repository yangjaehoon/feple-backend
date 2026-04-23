package com.feple.feple_backend.artistfollow.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.artistfollow.dto.FollowResponseDto;
import com.feple.feple_backend.artistfollow.dto.FollowStatusDto;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.global.exception.AuthenticationRequiredException;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

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

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new NoSuchElementException("아티스트를 찾을 수 없습니다. id=" + artistId));

        return new FollowStatusDto(followed, artist.getFollowerCount());
    }

    @Transactional
    public FollowResponseDto follow(Long userId, Long artistId) {
        if (userId == null) {
            throw new AuthenticationRequiredException("로그인이 필요합니다.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다. id=" + userId));

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new NoSuchElementException("아티스트를 찾을 수 없습니다. id=" + artistId));

        int followerCount = artist.getFollowerCount();
        if (!artistFollowRepository.existsByUserIdAndArtistId(userId, artistId)) {
            artistFollowRepository.save(ArtistFollow.of(user, artist));
            artistRepository.incrementFollowerCount(artistId);
            followerCount++;
        }

        return new FollowResponseDto(true, followerCount);
    }

    @Transactional
    public FollowResponseDto unfollow(Long userId, Long artistId) {
        if (userId == null) {
            throw new AuthenticationRequiredException("로그인이 필요합니다.");
        }

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new NoSuchElementException("아티스트를 찾을 수 없습니다. id=" + artistId));

        int followerCount = artist.getFollowerCount();
        if (artistFollowRepository.findByUserIdAndArtistId(userId, artistId).isPresent()) {
            artistFollowRepository.deleteByUserIdAndArtistId(userId, artistId);
            artistRepository.decrementFollowerCount(artistId);
            followerCount--;
        }

        return new FollowResponseDto(false, Math.max(followerCount, 0));
    }
}
