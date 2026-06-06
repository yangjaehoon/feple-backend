package com.feple.feple_backend.artistfollow.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfollow.dto.FollowResponseDto;
import com.feple.feple_backend.artistfollow.dto.FollowStatusDto;
import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataIntegrityViolationException;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArtistFollowServiceTest {

    @Mock ArtistFollowRepository artistFollowRepository;
    @Mock ArtistRepository artistRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ArtistFollowServiceImpl artistFollowService;

    private User user(Long id) {
        return User.builder().id(id).nickname("user" + id)
                .oauthId("o" + id).role(UserRole.USER).build();
    }

    private Artist artist(Long id, int followerCount) {
        return Artist.builder().id(id).name("아티스트" + id)
                .followerCount(followerCount).build();
    }

    // ── isFollowed ───────────────────────────────────────────────────

    @Test
    void 팔로우_중이면_true_반환() {
        given(artistFollowRepository.existsByUserIdAndArtistId(1L, 10L)).willReturn(true);

        assertThat(artistFollowService.isFollowed(1L, 10L)).isTrue();
    }

    @Test
    void 팔로우_안_했으면_false_반환() {
        given(artistFollowRepository.existsByUserIdAndArtistId(1L, 10L)).willReturn(false);

        assertThat(artistFollowService.isFollowed(1L, 10L)).isFalse();
    }

    // ── followStatus ─────────────────────────────────────────────────

    @Test
    void followStatus_userId_null이면_followed_false_반환() {
        given(artistRepository.findById(10L)).willReturn(Optional.of(artist(10L, 7)));

        FollowStatusDto result = artistFollowService.followStatus(null, 10L);

        assertThat(result.followed()).isFalse();
        assertThat(result.followerCount()).isEqualTo(7);
    }

    @Test
    void followStatus_팔로우_중이면_followed_true_반환() {
        Artist artist = artist(10L, 5);
        given(artistFollowRepository.existsByUserIdAndArtistId(1L, 10L)).willReturn(true);
        given(artistRepository.findById(10L)).willReturn(Optional.of(artist));

        FollowStatusDto result = artistFollowService.followStatus(1L, 10L);

        assertThat(result.followed()).isTrue();
        assertThat(result.followerCount()).isEqualTo(5);
    }

    @Test
    void followStatus_미팔로우이면_followed_false_반환() {
        Artist artist = artist(10L, 3);
        given(artistFollowRepository.existsByUserIdAndArtistId(1L, 10L)).willReturn(false);
        given(artistRepository.findById(10L)).willReturn(Optional.of(artist));

        FollowStatusDto result = artistFollowService.followStatus(1L, 10L);

        assertThat(result.followed()).isFalse();
        assertThat(result.followerCount()).isEqualTo(3);
    }

    // ── follow ───────────────────────────────────────────────────────

    @Test
    void 팔로우_성공시_saveAndFlush와_incrementFollowerCount_호출되고_followed_true_반환() {
        User user = user(1L);
        Artist artist = artist(10L, 0);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(artistRepository.findById(10L)).willReturn(Optional.of(artist));
        given(artistRepository.findFollowerCountById(10L)).willReturn(1);

        FollowResponseDto result = artistFollowService.follow(1L, 10L);

        assertThat(result.followed()).isTrue();
        assertThat(result.followerCount()).isEqualTo(1);
        verify(artistFollowRepository).saveAndFlush(any(ArtistFollow.class));
        verify(artistRepository).incrementFollowerCount(10L);
    }

    @Test
    void 이미_팔로우_중이면_saveAndFlush_예외를_잡아_멱등성_보장() {
        User user = user(1L);
        Artist artist = artist(10L, 5);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(artistRepository.findById(10L)).willReturn(Optional.of(artist));
        given(artistFollowRepository.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("duplicate"));
        given(artistRepository.findFollowerCountById(10L)).willReturn(5);

        FollowResponseDto result = artistFollowService.follow(1L, 10L);

        assertThat(result.followed()).isTrue();
        verify(artistRepository, never()).incrementFollowerCount(any());
    }

    @Test
    void 존재하지_않는_아티스트_팔로우시_예외() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(artistRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> artistFollowService.follow(1L, 99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── unfollow ─────────────────────────────────────────────────────

    @Test
    void 언팔로우_성공시_delete와_decrementFollowerCount_호출되고_followed_false_반환() {
        given(artistRepository.existsById(10L)).willReturn(true);
        given(artistFollowRepository.deleteByUserIdAndArtistId(1L, 10L)).willReturn(1);
        given(artistRepository.findFollowerCountById(10L)).willReturn(0);

        FollowResponseDto result = artistFollowService.unfollow(1L, 10L);

        assertThat(result.followed()).isFalse();
        assertThat(result.followerCount()).isEqualTo(0);
        verify(artistFollowRepository).deleteByUserIdAndArtistId(1L, 10L);
        verify(artistRepository).decrementFollowerCount(10L);
    }

    @Test
    void 팔로우_안_했을_때_언팔로우시_decrement_미호출_멱등성_보장() {
        given(artistRepository.existsById(10L)).willReturn(true);
        given(artistFollowRepository.deleteByUserIdAndArtistId(1L, 10L)).willReturn(0);
        given(artistRepository.findFollowerCountById(10L)).willReturn(0);

        FollowResponseDto result = artistFollowService.unfollow(1L, 10L);

        assertThat(result.followed()).isFalse();
        verify(artistFollowRepository).deleteByUserIdAndArtistId(1L, 10L);
        verify(artistRepository, never()).decrementFollowerCount(any());
    }

    @Test
    void 존재하지_않는_아티스트_언팔로우시_예외() {
        given(artistRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> artistFollowService.unfollow(1L, 99L))
                .isInstanceOf(NoSuchElementException.class);
    }
}
