package com.feple.feple_backend.artist.song.service;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.dto.SongRequestResponseDto;
import com.feple.feple_backend.artist.song.dto.SubmitSongRequestDto;
import com.feple.feple_backend.artist.song.entity.SongRequest;
import com.feple.feple_backend.artist.song.entity.SongRequestStatus;
import com.feple.feple_backend.artist.song.repository.SongRepository;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SongRequestServiceTest {

    @Mock SongRequestRepository songRequestRepository;
    @Mock ArtistRepository artistRepository;
    @Mock UserRepository userRepository;
    @Mock YoutubeSearchService youtubeSearchService;
    @Mock SongRepository songRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks SongRequestServiceImpl songRequestService;

    private Artist artist(Long id) {
        return Artist.builder().id(id).name("아이유").build();
    }

    private User user(Long id) {
        return User.builder().id(id).nickname("user" + id)
                .oauthId("o" + id).role(UserRole.USER).build();
    }

    private SubmitSongRequestDto dto(String title) {
        SubmitSongRequestDto dto = new SubmitSongRequestDto();
        dto.setSongTitle(title);
        return dto;
    }

    private SongRequest savedRequest(Long id, Artist artist, Long userId, String title) {
        return SongRequest.builder()
                .id(id).artist(artist).userId(userId)
                .songTitle(title).status(SongRequestStatus.PENDING).build();
    }

    // ── submit ────────────────────────────────────────────────────────

    @Test
    void 이미_요청한_곡_재요청시_ConflictException() {
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist(1L)));
        given(songRequestRepository.existsByArtistIdAndUserIdAndSongTitleIgnoreCaseAndStatus(
                1L, 10L, "Blueming", SongRequestStatus.PENDING)).willReturn(true);

        assertThatThrownBy(() -> songRequestService.submit(1L, 10L, dto("Blueming")))
                .isInstanceOf(ConflictException.class);

        verify(songRequestRepository, never()).save(any());
    }

    @Test
    void 신규_곡_요청시_저장됨() {
        Artist artist = artist(1L);
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));
        given(songRequestRepository.existsByArtistIdAndUserIdAndSongTitleIgnoreCaseAndStatus(
                1L, 10L, "Lilac", SongRequestStatus.PENDING)).willReturn(false);
        given(userRepository.findById(10L)).willReturn(Optional.of(user(10L)));
        SongRequest saved = savedRequest(5L, artist, 10L, "Lilac");
        given(songRequestRepository.save(any(SongRequest.class))).willReturn(saved);

        SongRequestResponseDto result = songRequestService.submit(1L, 10L, dto("Lilac"));

        assertThat(result.getSongTitle()).isEqualTo("Lilac");
        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(songRequestRepository).save(any(SongRequest.class));
    }

    @Test
    void 존재하지_않는_아티스트_요청시_예외() {
        given(artistRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> songRequestService.submit(99L, 10L, dto("어떤 곡")))
                .isInstanceOf(NoSuchElementException.class);

        verify(songRequestRepository, never()).save(any());
    }

    @Test
    void 대소문자_무관_중복_요청_거부됨() {
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist(1L)));
        given(songRequestRepository.existsByArtistIdAndUserIdAndSongTitleIgnoreCaseAndStatus(
                1L, 10L, "blueming", SongRequestStatus.PENDING)).willReturn(true);

        assertThatThrownBy(() -> songRequestService.submit(1L, 10L, dto("blueming")))
                .isInstanceOf(ConflictException.class);
    }

    // ── approve / reject ──────────────────────────────────────────────

    @Test
    void 승인시_상태가_APPROVED로_변경됨() {
        Artist artist = artist(1L);
        SongRequest request = savedRequest(1L, artist, 10L, "Lilac");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));

        // youtubeUrl null → youtube 조회 없이 바로 승인
        songRequestService.approve(1L, null);

        assertThat(request.getStatus()).isEqualTo(SongRequestStatus.APPROVED);
    }

    @Test
    void 거절시_상태가_REJECTED로_변경됨() {
        Artist artist = artist(1L);
        SongRequest request = savedRequest(1L, artist, 10L, "Lilac");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));

        songRequestService.reject(1L, "저작권 문제");

        assertThat(request.getStatus()).isEqualTo(SongRequestStatus.REJECTED);
    }

    @Test
    void 존재하지_않는_요청_승인시_예외() {
        given(songRequestRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> songRequestService.approve(99L, null))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── countPending ──────────────────────────────────────────────────

    @Test
    void getPendingCount_레포지토리에_위임됨() {
        given(songRequestRepository.countByStatus(SongRequestStatus.PENDING)).willReturn(3L);

        assertThat(songRequestService.getPendingCount()).isEqualTo(3L);
    }
}
