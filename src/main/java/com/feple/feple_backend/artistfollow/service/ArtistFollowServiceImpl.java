package com.feple.feple_backend.artistfollow.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.artistfollow.dto.FollowResponseDto;
import com.feple.feple_backend.artistfollow.dto.FollowStatusDto;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistFollowServiceImpl implements ArtistFollowService {

    private final ArtistFollowRepository artistFollowRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;

    @Override
    public boolean isFollowed(Long userId, Long artistId) {
        return artistFollowRepository.existsByUserIdAndArtistId(userId, artistId);
    }

    @Override
    public List<Long> getFollowerUserIds(Long artistId) {
        return artistFollowRepository.findByArtistId(artistId)
                .stream().map(ArtistFollow::getUserId).toList();
    }

    @Override
    public FollowStatusDto followStatus(Long userId, Long artistId) {
        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, artistId, "아티스트");
        boolean followed = userId != null && artistFollowRepository.existsByUserIdAndArtistId(userId, artistId);
        return new FollowStatusDto(followed, artist.getFollowerCount());
    }

    @Override
    @Transactional
    public FollowResponseDto follow(Long userId, Long artistId) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, artistId, "아티스트");

        if (!artistFollowRepository.existsByUserIdAndArtistId(userId, artistId)) {
            try {
                artistFollowRepository.saveAndFlush(ArtistFollow.of(user, artist));
                artistRepository.incrementFollowerCount(artistId);
                artist = artistRepository.findById(artistId).orElse(artist);
            } catch (DataIntegrityViolationException ignored) {
                // 동시 요청으로 이미 팔로우됨 — 카운터 그대로
            }
        }
        return new FollowResponseDto(true, artist.getFollowerCount());
    }

    @Override
    @Transactional
    public FollowResponseDto unfollow(Long userId, Long artistId) {
        int deleted = artistFollowRepository.deleteByUserIdAndArtistId(userId, artistId);
        if (deleted > 0) {
            artistRepository.decrementFollowerCount(artistId);
        }
        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, artistId, "아티스트");
        return new FollowResponseDto(false, artist.getFollowerCount());
    }

    @Override
    @Transactional
    public void removeAllByUser(Long userId) {
        artistFollowRepository.decrementFollowerCountByUserId(userId);
        artistFollowRepository.deleteByUserId(userId);
    }
}
