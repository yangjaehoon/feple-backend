package com.feple.feple_backend.artistfollow.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfollow.dto.FollowResponseDto;
import com.feple.feple_backend.artistfollow.dto.FollowStatusDto;
import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtistFollowServiceImpl implements ArtistFollowService {

    private final ArtistFollowRepository artistFollowRepository;
    private final ArtistRepository artistRepository;
    private final UserRepository userRepository;

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
    // ArtistServiceImpl.getArtistById가 10분 캐싱하는 팔로워 수(followerCount)가 이 메서드로
    // 갱신되므로, 같은 캐시를 여기서도 evict해야 상세 화면에 반영된다.
    @CacheEvict(value = "artistDetail", key = "#artistId")
    public FollowResponseDto follow(Long userId, Long artistId) {
        User user = EntityLoader.getOrThrow(userRepository::findById, userId, "사용자");
        Artist artist = EntityLoader.getOrThrow(artistRepository::findById, artistId, "아티스트");

        if (!artistFollowRepository.existsByUserIdAndArtistId(userId, artistId)) {
            try {
                artistFollowRepository.saveAndFlush(ArtistFollow.of(user, artist));
                artistRepository.incrementFollowerCount(artistId);
            } catch (DataIntegrityViolationException ignored) {
                // 동시 요청으로 이미 팔로우됨 — 카운터 그대로
            }
        }
        return new FollowResponseDto(true, currentFollowerCount(artistId));
    }

    private int currentFollowerCount(Long artistId) {
        Integer count = artistRepository.findFollowerCountById(artistId);
        if (count == null) {
            throw new NoSuchElementException("아티스트을(를) 찾을 수 없습니다: " + artistId);
        }
        return count;
    }

    @Override
    @Transactional
    @CacheEvict(value = "artistDetail", key = "#artistId")
    public FollowResponseDto unfollow(Long userId, Long artistId) {
        int deleted = artistFollowRepository.deleteByUserIdAndArtistId(userId, artistId);
        if (deleted > 0) {
            artistRepository.decrementFollowerCount(artistId);
        }
        return new FollowResponseDto(false, currentFollowerCount(artistId));
    }

    @Override
    @Transactional
    // 탈퇴한 유저가 팔로우하던 아티스트가 몇 명인지 알 수 없어 특정 키만 evict할 수
    // 없다 — 계정 삭제라는 드문 경로이므로 전체 evict 비용은 감수할 만하다.
    @CacheEvict(value = "artistDetail", allEntries = true)
    public void removeAllByUser(Long userId) {
        artistFollowRepository.decrementFollowerCountByUserId(userId);
        artistFollowRepository.deleteByUserId(userId);
    }

    @Override
    @Transactional
    public void removeAllByArtist(Long artistId) {
        artistFollowRepository.deleteByArtistId(artistId);
    }
}
