package com.feple.feple_backend.artistfollow.service;

import com.feple.feple_backend.artist.domain.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfollow.domain.ArtistFollow;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.user.domain.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArtistFollowService {

    private final ArtistFollowRepository artistFollowRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;

    @Transactional
    public void follow(Long userId, Long artistId) {
        // 존재 검증(없으면 예외)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Artist not found"));

        try {
            artistFollowRepository.save(ArtistFollow.of(user, artist));
            artistRepository.incrementFollowerCount(artistId);
        } catch (DataIntegrityViolationException e) {
            // UNIQUE(user_id, artist_id) 위반 = 이미 팔로우 상태 -> 멱등 처리
            // count 증가하면 안 됨
        }
    }

    @Transactional
    public void unfollow(Long userId, Long artistId) {
        artistFollowRepository.findByUserIdAndArtistId(userId, artistId)
                .ifPresent(follow -> {
                    artistFollowRepository.delete(follow);
                    artistRepository.decrementFollowerCount(artistId);
                });
        // 없으면 아무 것도 안 함 = 멱등
    }

    @Transactional(readOnly = true)
    public boolean isFollowed(Long userId, Long artistId) {
        return artistFollowRepository.existsByUserIdAndArtistId(userId, artistId);
    }
}