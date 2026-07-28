package com.feple.feple_backend.artist.song.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class SongRequestServiceTest {

    @Mock SongRequestRepository songRequestRepository;
    @Mock ArtistRepository artistRepository;
    @Mock UserNicknameLookup nicknameResolver;
    @Mock YoutubeSearchService youtubeSearchService;
    @Mock SongRepository songRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks SongRequestServiceImpl songRequestService;

    private Artist artist(Long id) {
        return Artist.builder().id(id).name("아이유").build();
    }

    private SubmitSongRequestDto dto(String title) {
        SubmitSongRequestDto dto = mock(SubmitSongRequestDto.class);
        lenient().when(dto.getSongTitle()).thenReturn(title);
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
        given(nicknameResolver.lookup(10L)).willReturn("user10");
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
        songRequestService.approveAndMaybeSaveSong(1L, null);

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
    void 이미_승인된_요청_재승인시_예외() {
        Artist artist = artist(1L);
        SongRequest request = SongRequest.builder()
                .id(1L).artist(artist).userId(10L)
                .songTitle("Lilac").status(SongRequestStatus.APPROVED).build();
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> songRequestService.approveAndMaybeSaveSong(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 처리된 노래 요청입니다.");
    }

    @Test
    void 이미_거절된_요청_재거절시_예외() {
        Artist artist = artist(1L);
        SongRequest request = SongRequest.builder()
                .id(1L).artist(artist).userId(10L)
                .songTitle("Lilac").status(SongRequestStatus.REJECTED).build();
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));

        assertThatThrownBy(() -> songRequestService.reject(1L, "사유"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 처리된 노래 요청입니다.");
    }

    @Test
    void 존재하지_않는_요청_승인시_예외() {
        given(songRequestRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> songRequestService.approveAndMaybeSaveSong(99L, null))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ── getPendingCount / getTotalCount ────────────────────────────────

    @Test
    void getPendingCount_레포지토리에_위임됨() {
        given(songRequestRepository.countByStatus(SongRequestStatus.PENDING)).willReturn(3L);

        assertThat(songRequestService.getPendingCount()).isEqualTo(3L);
    }

    @Test
    void getTotalCount_레포지토리에_위임됨() {
        given(songRequestRepository.count()).willReturn(20L);

        assertThat(songRequestService.getTotalCount()).isEqualTo(20L);
    }

    // ── getMyAllRequests / getMyRequests ─────────────────────────────

    @Test
    void 내_전체_요청목록_조회() {
        Artist artist = artist(1L);
        SongRequest request = savedRequest(1L, artist, 10L, "Lilac");
        given(nicknameResolver.lookup(10L)).willReturn("user10");
        given(songRequestRepository.findByUserIdOrderByCreatedAtDesc(10L)).willReturn(List.of(request));

        var result = songRequestService.getMyAllRequests(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSongTitle()).isEqualTo("Lilac");
    }

    @Test
    void 특정_아티스트에_대한_내_요청목록_조회() {
        Artist artist = artist(1L);
        SongRequest request = savedRequest(1L, artist, 10L, "Lilac");
        given(nicknameResolver.lookup(10L)).willReturn("user10");
        given(songRequestRepository.findByArtistIdAndUserIdOrderByCreatedAtDesc(1L, 10L)).willReturn(List.of(request));

        var result = songRequestService.getMyRequests(1L, 10L);

        assertThat(result).hasSize(1);
    }

    // ── getRequestsPage ───────────────────────────────────────────────

    @Test
    void 상태_ALL이면_필터없이_전체_조회() {
        given(songRequestRepository.findWithFilters(null, null, PageRequest.of(0, 10)))
                .willReturn(new PageImpl<>(List.of()));
        given(nicknameResolver.buildMap(anyList(), org.mockito.ArgumentMatchers.any())).willReturn(Map.of());

        Page<SongRequestResponseDto> result = songRequestService.getRequestsPage(0, 10, "ALL", null);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void 상태_유효한_값이면_해당_상태로_필터() {
        given(songRequestRepository.findWithFilters(SongRequestStatus.APPROVED, null, PageRequest.of(0, 10)))
                .willReturn(new PageImpl<>(List.of()));
        given(nicknameResolver.buildMap(anyList(), org.mockito.ArgumentMatchers.any())).willReturn(Map.of());

        songRequestService.getRequestsPage(0, 10, "APPROVED", null);

        verify(songRequestRepository).findWithFilters(SongRequestStatus.APPROVED, null, PageRequest.of(0, 10));
    }

    @Test
    void 상태_잘못된_값이면_PENDING으로_폴백() {
        given(songRequestRepository.findWithFilters(SongRequestStatus.PENDING, null, PageRequest.of(0, 10)))
                .willReturn(new PageImpl<>(List.of()));
        given(nicknameResolver.buildMap(anyList(), org.mockito.ArgumentMatchers.any())).willReturn(Map.of());

        songRequestService.getRequestsPage(0, 10, "INVALID_STATUS", null);

        verify(songRequestRepository).findWithFilters(SongRequestStatus.PENDING, null, PageRequest.of(0, 10));
    }

    @Test
    void 닉네임_매핑에_없는_사용자는_UNKNOWN으로_표시() {
        Artist artist = artist(1L);
        SongRequest request = savedRequest(1L, artist, 10L, "Lilac");
        given(songRequestRepository.findWithFilters(null, null, PageRequest.of(0, 10)))
                .willReturn(new PageImpl<>(List.of(request)));
        given(nicknameResolver.buildMap(anyList(), org.mockito.ArgumentMatchers.any())).willReturn(Map.of());

        Page<SongRequestResponseDto> result = songRequestService.getRequestsPage(0, 10, null, null);

        assertThat(result.getContent().get(0).getUserNickname()).isEqualTo(UserNicknameLookup.UNKNOWN);
    }

    // ── getPendingRequests / getPendingPreview ────────────────────────

    @Test
    void 대기중인_요청목록_조회() {
        Artist artist = artist(1L);
        SongRequest request = savedRequest(1L, artist, 10L, "Lilac");
        given(songRequestRepository.findByArtistIdAndStatusOrderByCreatedAtDesc(1L, SongRequestStatus.PENDING))
                .willReturn(List.of(request));
        given(nicknameResolver.buildMap(anyList(), org.mockito.ArgumentMatchers.any())).willReturn(Map.of(10L, "user10"));

        var result = songRequestService.getPendingRequests(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserNickname()).isEqualTo("user10");
    }

    @Test
    void 대기중인_요청_미리보기_조회() {
        Artist artist = artist(1L);
        SongRequest request = savedRequest(1L, artist, 10L, "Lilac");
        given(songRequestRepository.findByStatusOrderByCreatedAtDesc(SongRequestStatus.PENDING, PageRequest.of(0, 5)))
                .willReturn(List.of(request));

        List<SongRequest> result = songRequestService.getPendingPreview(5);

        assertThat(result).hasSize(1);
    }

    // ── approveAndMaybeSaveSong: youtube 연동 분기 ────────────────────

    @Test
    void 승인시_이벤트_발행됨() {
        Artist artist = artist(1L);
        SongRequest request = savedRequest(1L, artist, 10L, "Lilac");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));

        songRequestService.approveAndMaybeSaveSong(1L, null);

        verify(eventPublisher).publishEvent(any(SongRequestApprovedEvent.class));
    }

    @Test
    void 승인시_유튜브URL_공백이면_곡_저장_스킵() {
        Artist artist = artist(1L);
        SongRequest request = savedRequest(1L, artist, 10L, "Lilac");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));

        boolean songSaved = songRequestService.approveAndMaybeSaveSong(1L, "  ");

        assertThat(songSaved).isFalse();
        verify(songRepository, never()).save(any());
    }

    @Test
    void 승인시_유튜브_영상_조회안되면_곡_저장_스킵() {
        Artist artist = artist(1L);
        SongRequest request = savedRequest(1L, artist, 10L, "Lilac");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));
        given(youtubeSearchService.fetchVideoByUrl("https://youtube.com/watch?v=abc")).willReturn(Optional.empty());

        boolean songSaved = songRequestService.approveAndMaybeSaveSong(1L, "https://youtube.com/watch?v=abc");

        assertThat(songSaved).isFalse();
        verify(songRepository, never()).save(any());
    }

    @Test
    void 승인시_이미_등록된_영상이면_곡_저장_스킵() {
        Artist artist = artist(1L);
        SongRequest request = savedRequest(1L, artist, 10L, "Lilac");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));
        YoutubeVideoDto video = YoutubeVideoDto.builder().videoId("abc").title("Lilac").thumbnailUrl("thumb.jpg").build();
        given(youtubeSearchService.fetchVideoByUrl("https://youtube.com/watch?v=abc")).willReturn(Optional.of(video));
        given(songRepository.existsByYoutubeVideoIdAndArtistId("abc", 1L)).willReturn(true);

        boolean songSaved = songRequestService.approveAndMaybeSaveSong(1L, "https://youtube.com/watch?v=abc");

        assertThat(songSaved).isFalse();
        verify(songRepository, never()).save(any());
    }

    @Test
    void 승인시_신규_영상이면_곡_저장됨() {
        Artist artist = artist(1L);
        SongRequest request = savedRequest(1L, artist, 10L, "Lilac");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));
        YoutubeVideoDto video = YoutubeVideoDto.builder().videoId("abc").title("Lilac").thumbnailUrl("thumb.jpg").build();
        given(youtubeSearchService.fetchVideoByUrl("https://youtube.com/watch?v=abc")).willReturn(Optional.of(video));
        given(songRepository.existsByYoutubeVideoIdAndArtistId("abc", 1L)).willReturn(false);

        boolean songSaved = songRequestService.approveAndMaybeSaveSong(1L, "https://youtube.com/watch?v=abc");

        assertThat(songSaved).isTrue();
        verify(songRepository).save(any(Song.class));
        assertThat(request.getYoutubeUrl()).isEqualTo("https://youtube.com/watch?v=abc");
    }

    // ── reject ────────────────────────────────────────────────────────

    @Test
    void 거절시_이벤트_발행됨() {
        Artist artist = artist(1L);
        SongRequest request = savedRequest(1L, artist, 10L, "Lilac");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));

        songRequestService.reject(1L, "저작권 문제");

        verify(eventPublisher).publishEvent(any(SongRequestRejectedEvent.class));
    }

    // ── removeAllByUser ───────────────────────────────────────────────

    @Test
    void 사용자_노래요청_전체_삭제() {
        songRequestService.removeAllByUser(10L);

        verify(songRequestRepository).deleteByUserId(10L);
    }
}
