package com.feple.feple_backend.artistfollow.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.artistfollow.dto.FollowResponseDto;
import com.feple.feple_backend.artistfollow.dto.FollowStatusDto;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.user.domain.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "userId is null");
        }

        boolean followed = artistFollowRepository.existsByUserIdAndArtistId(userId, artistId); // [web:569]

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "artist not found"));

        return new FollowStatusDto(followed, artist.getFollowerCount());
    }

    @Transactional
    public FollowResponseDto follow(Long userId, Long artistId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Artist not found"));

        boolean followed = false;

        try {
            artistFollowRepository.save(ArtistFollow.of(user, artist));
            artistRepository.incrementFollowerCount(artistId);
            followed = true;
        } catch (DataIntegrityViolationException e) {
            // 이미 팔로우면 멱등: followed=false로 둘지 true로 둘지 정책인데,
            // UX상 이미 팔로우 상태면 true로 응답하는 게 보통 더 자연스러움.
            followed = true;
        }

        int count = artistRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Artist not found"))
                .getFollowerCount();

        return new FollowResponseDto(followed, count);
    }

    @Transactional
    public FollowResponseDto unfollow(Long userId, Long artistId) {
        artistFollowRepository.findByUserIdAndArtistId(userId, artistId)
                .ifPresent(follow -> {
                    artistFollowRepository.delete(follow);
                    artistRepository.decrementFollowerCount(artistId);
                });

        int count = artistRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Artist not found"))
                .getFollowerCount();

        return new FollowResponseDto(false, count);
    }
}