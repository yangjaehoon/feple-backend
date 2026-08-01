package com.feple.feple_backend.artist.song.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.dto.SongRequestResponseDto;
import com.feple.feple_backend.artist.song.dto.SubmitSongRequestDto;
import com.feple.feple_backend.artist.song.dto.YoutubeVideoDto;
import com.feple.feple_backend.artist.song.entity.Song;
import com.feple.feple_backend.artist.song.entity.SongRequest;
import com.feple.feple_backend.artist.song.entity.SongRequestStatus;
import com.feple.feple_backend.artist.song.event.SongRequestApprovedEvent;
import com.feple.feple_backend.artist.song.event.SongRequestRejectedEvent;
import com.feple.feple_backend.artist.song.repository.SongRepository;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.global.UserNicknameLookup;
import com.feple.feple_backend.global.exception.ConflictException;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SongRequestServiceImplTest {

    @Mock SongRequestRepository songRequestRepository;
    @Mock ArtistRepository artistRepository;
    @Mock UserNicknameLookup nicknameResolver;
    @Mock YoutubeSearchService youtubeSearchService;
    @Mock SongRepository songRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    private SongRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SongRequestServiceImpl(
                songRequestRepository, artistRepository, nicknameResolver, youtubeSearchService, songRepository, eventPublisher);
    }

    private Artist artist(Long id, String name) {
        return Artist.builder().id(id).name(name).build();
    }

    private SongRequest pending(Long id, Artist artist, Long userId, String songTitle) {
        return SongRequest.builder()
                .id(id).artist(artist).userId(userId).songTitle(songTitle).status(SongRequestStatus.PENDING).build();
    }

    // ── submit ───────────────────────────────────────────────────────────

    @Test
    void 요청_정상_제출() {
        Artist artist = artist(1L, "아이유");
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));
        SubmitSongRequestDto dto = new SubmitSongRequestDto();
        ReflectionTestUtils.setField(dto, "songTitle", "밤편지");
        given(songRequestRepository.existsByArtistIdAndUserIdAndSongTitleIgnoreCaseAndStatus(
                1L, 10L, "밤편지", SongRequestStatus.PENDING)).willReturn(false);
        SongRequest saved = pending(1L, artist, 10L, "밤편지");
        given(songRequestRepository.save(any(SongRequest.class))).willReturn(saved);
        given(nicknameResolver.lookup(10L)).willReturn("닉네임");

        SongRequestResponseDto result = service.submit(1L, 10L, dto);

        assertThat(result.getSongTitle()).isEqualTo("밤편지");
        assertThat(result.getUserNickname()).isEqualTo("닉네임");
    }

    @Test
    void 요청_이미_동일곡_대기중이면_예외() {
        Artist artist = artist(1L, "아이유");
        given(artistRepository.findById(1L)).willReturn(Optional.of(artist));
        SubmitSongRequestDto dto = new SubmitSongRequestDto();
        ReflectionTestUtils.setField(dto, "songTitle", "밤편지");
        given(songRequestRepository.existsByArtistIdAndUserIdAndSongTitleIgnoreCaseAndStatus(
                1L, 10L, "밤편지", SongRequestStatus.PENDING)).willReturn(true);

        assertThatThrownBy(() -> service.submit(1L, 10L, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("이미 요청한");
    }

    // ── approveAndMaybeSaveSong ──────────────────────────────────────────

    @Test
    void 승인_유튜브URL_없으면_곡_저장_안함() {
        Artist artist = artist(1L, "아이유");
        SongRequest request = pending(1L, artist, 10L, "밤편지");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));

        boolean saved = service.approveAndMaybeSaveSong(1L, null);

        assertThat(saved).isFalse();
        assertThat(request.isPending()).isFalse();
        verify(songRepository, never()).save(any());
        verify(eventPublisher).publishEvent(any(SongRequestApprovedEvent.class));
    }

    @Test
    void 승인_유튜브URL_있고_중복곡_아니면_곡_저장() {
        Artist artist = artist(1L, "아이유");
        SongRequest request = pending(1L, artist, 10L, "밤편지");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));
        YoutubeVideoDto video = YoutubeVideoDto.builder()
                .videoId("abc123").title("밤편지 MV").thumbnailUrl("https://thumb").build();
        given(youtubeSearchService.fetchVideoByUrl("https://youtu.be/abc123")).willReturn(Optional.of(video));
        given(songRepository.existsByYoutubeVideoIdAndArtistId("abc123", 1L)).willReturn(false);

        boolean saved = service.approveAndMaybeSaveSong(1L, "https://youtu.be/abc123");

        assertThat(saved).isTrue();
        assertThat(request.getYoutubeUrl()).isEqualTo("https://youtu.be/abc123");
        verify(songRepository).save(any(Song.class));
        ArgumentCaptor<SongRequestApprovedEvent> captor = ArgumentCaptor.forClass(SongRequestApprovedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(10L);
        assertThat(captor.getValue().artistId()).isEqualTo(1L);
    }

    @Test
    void 승인_이미_등록된_영상이면_곡_저장_안함() {
        Artist artist = artist(1L, "아이유");
        SongRequest request = pending(1L, artist, 10L, "밤편지");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));
        YoutubeVideoDto video = YoutubeVideoDto.builder().videoId("abc123").title("밤편지 MV").build();
        given(youtubeSearchService.fetchVideoByUrl("https://youtu.be/abc123")).willReturn(Optional.of(video));
        given(songRepository.existsByYoutubeVideoIdAndArtistId("abc123", 1L)).willReturn(true);

        boolean saved = service.approveAndMaybeSaveSong(1L, "https://youtu.be/abc123");

        assertThat(saved).isFalse();
        verify(songRepository, never()).save(any());
    }

    @Test
    void 승인_존재하지_않는_요청이면_예외() {
        given(songRequestRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.approveAndMaybeSaveSong(1L, null))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 승인_이미_처리된_요청이면_예외() {
        Artist artist = artist(1L, "아이유");
        SongRequest request = pending(1L, artist, 10L, "밤편지");
        request.approve();
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> service.approveAndMaybeSaveSong(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 처리된");
    }

    // ── reject ───────────────────────────────────────────────────────────

    @Test
    void 반려시_상태변경후_이벤트_발행() {
        Artist artist = artist(1L, "아이유");
        SongRequest request = pending(1L, artist, 10L, "밤편지");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));

        service.reject(1L, "저작권 문제");

        assertThat(request.isPending()).isFalse();
        ArgumentCaptor<SongRequestRejectedEvent> captor = ArgumentCaptor.forClass(SongRequestRejectedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().reason()).isEqualTo("저작권 문제");
    }

    @Test
    void 반려시_이미_처리된_요청이면_예외() {
        Artist artist = artist(1L, "아이유");
        SongRequest request = pending(1L, artist, 10L, "밤편지");
        request.reject();
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> service.reject(1L, "다시반려"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 처리된");
    }

    // ── removeAllByUser ──────────────────────────────────────────────────

    @Test
    void 회원탈퇴시_전체_요청_삭제() {
        service.removeAllByUser(10L);

        verify(songRequestRepository).deleteByUserId(10L);
    }
}
