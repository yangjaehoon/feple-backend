package com.feple.feple_backend.artistfollow.service;

import com.feple.feple_backend.artist.domain.Artist;
import com.feple.feple_backend.artistfollow.domain.ArtistFollow;
import com.feple.feple_backend.user.domain.User;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

public class ArtistfollowService {

    @Transactional
    public FollowResult follow(Long userId, Long artistId) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new NotFoundException("artist"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user"));

        try {
            artistFollowRepository.save(ArtistFollow.of(user, artist));
            artist.increaseFollowCount();
            return FollowResult.followed(artist.getFollowCount());
        } catch (DataIntegrityViolationException e) {
            return FollowResult.alreadyFollowed(artist.getFollowCount());
        }
    }

    @Transactional
    public FollowResult unfollow(Long userId, Long artistId) {
        ArtistFollow follow = artistFollowRepository
                .findByUserIdAndArtistId(userId, artistId)
                .orElse(null);

        if (follow == null) {
            int count = artistRepository.findFollowCount(artistId);
            return FollowResult.alreadyUnfollowed(count);
        }

        Artist artist = follow.getArtist();
        artistFollowRepository.delete(follow);
        artist.decreaseFollowCount();

        return FollowResult.unfollowed(artist.getFollowCount());
    }
}
