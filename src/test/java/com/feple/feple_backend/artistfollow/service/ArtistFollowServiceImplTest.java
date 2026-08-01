package com.feple.feple_backend.artistfollow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfollow.dto.FollowResponseDto;
import com.feple.feple_backend.artistfollow.dto.FollowStatusDto;
import com.feple.feple_backend.artistfollow.entity.ArtistFollow;
import com.feple.feple_backend.artistfollow.repository.ArtistFollowRepository;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ArtistFollowServiceImplTest {

    @Mock ArtistFollowRepository artistFollowRepository;
    @Mock ArtistRepository artistRepository;
    @Mock UserRepository userRepository;

    private ArtistFollowServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ArtistFollowServiceImpl(artistFollowRepository, artistRepository, userRepository);
    }

    private Artist artist(Long id, int followerCount) {
        return Artist.builder().id(id).name("아이유").followerCount(followerCount).build();
    }

    private User user(Long id) {
        return User.builder().id(id).build();
    }

    // ── getFollowerUserIds ──────────────────────────────────────────────

    @Test
    void 팔로워_유저ID_목록_조회() {
        User u1 = user(10L);
        User u2 = user(11L);
        Artist artist = artist(1L, 2);
        given(artistFollowRepository.findByArtistId(1L)).willReturn(List.of(
                ArtistFollow.of(u1, artist), ArtistFollow.of(u2, artist)));

        List<Long> result = service.getFollowerUserIds(1L);

        assertThat(result).containsExactly(10L, 11L);
    }

    // ── followStatus ─────────────────────────────────────────────────────

    @Test
    void 팔로우상태_로그인유저_팔로우중() {
        Artist artist = artist(1L, 5);
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));
        given(artistFollowRepository.existsByUserIdAndArtistId(10L, 1L)).willReturn(true);

        FollowStatusDto result = service.followStatus(10L, 1L);

        assertThat(result.followed()).isTrue();
        assertThat(result.followerCount()).isEqualTo(5);
    }

    @Test
    void 팔로우상태_비로그인이면_미팔로우_고정() {
        Artist artist = artist(1L, 5);
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));

        FollowStatusDto result = service.followStatus(null, 1L);

        assertThat(result.followed()).isFalse();
    }

    // ── follow ───────────────────────────────────────────────────────────

    @Test
    void 팔로우_최초_팔로우시_카운트_증가() {
        User user = user(10L);
        Artist artist = artist(1L, 5);
        Artist afterIncrement = artist(1L, 6);
        given(userRepository.findById(10L)).willReturn(Optional.of(user));
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist), Optional.of(afterIncrement));
        given(artistFollowRepository.existsByUserIdAndArtistId(10L, 1L)).willReturn(false);

        FollowResponseDto result = service.follow(10L, 1L);

        assertThat(result.followed()).isTrue();
        assertThat(result.followerCount()).isEqualTo(6);
        verify(artistFollowRepository).saveAndFlush(any());
        verify(artistRepository).incrementFollowerCount(1L);
    }

    @Test
    void 팔로우_이미_팔로우중이면_중복저장_안함() {
        User user = user(10L);
        Artist artist = artist(1L, 5);
        given(userRepository.findById(10L)).willReturn(Optional.of(user));
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));
        given(artistFollowRepository.existsByUserIdAndArtistId(10L, 1L)).willReturn(true);

        FollowResponseDto result = service.follow(10L, 1L);

        assertThat(result.followed()).isTrue();
        assertThat(result.followerCount()).isEqualTo(5);
        verify(artistFollowRepository, never()).saveAndFlush(any());
        verify(artistRepository, never()).incrementFollowerCount(1L);
    }

    @Test
    void 팔로우_동시요청으로_이미팔로우된경우_예외무시하고_카운트유지() {
        User user = user(10L);
        Artist artist = artist(1L, 5);
        given(userRepository.findById(10L)).willReturn(Optional.of(user));
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));
        given(artistFollowRepository.existsByUserIdAndArtistId(10L, 1L)).willReturn(false);
        given(artistFollowRepository.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("dup"));

        FollowResponseDto result = service.follow(10L, 1L);

        assertThat(result.followed()).isTrue();
        assertThat(result.followerCount()).isEqualTo(5);
        verify(artistRepository, never()).incrementFollowerCount(1L);
    }

    // ── unfollow ─────────────────────────────────────────────────────────

    @Test
    void 언팔로우_삭제된경우_카운트_감소() {
        Artist afterDecrement = artist(1L, 4);
        given(artistFollowRepository.deleteByUserIdAndArtistId(10L, 1L)).willReturn(1);
        given(artistRepository.findById(1L)).willReturn(Optional.of(afterDecrement));

        FollowResponseDto result = service.unfollow(10L, 1L);

        assertThat(result.followed()).isFalse();
        assertThat(result.followerCount()).isEqualTo(4);
        verify(artistRepository).decrementFollowerCount(1L);
    }

    @Test
    void 언팔로우_삭제된게_없으면_카운트_감소_안함() {
        Artist artist = artist(1L, 5);
        given(artistFollowRepository.deleteByUserIdAndArtistId(10L, 1L)).willReturn(0);
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));

        FollowResponseDto result = service.unfollow(10L, 1L);

        assertThat(result.followerCount()).isEqualTo(5);
        verify(artistRepository, never()).decrementFollowerCount(1L);
    }

    // ── removeAllByUser / removeAllByArtist ────────────────────────────

    @Test
    void 회원탈퇴시_팔로우_카운트_감소후_삭제() {
        service.removeAllByUser(10L);

        verify(artistFollowRepository).decrementFollowerCountByUserId(10L);
        verify(artistFollowRepository).deleteByUserId(10L);
    }

    @Test
    void 아티스트삭제시_팔로우_전체삭제() {
        service.removeAllByArtist(1L);

        verify(artistFollowRepository).deleteByArtistId(1L);
    }
}
