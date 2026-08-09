package com.feple.feple_backend.artist.song.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artist.song.dto.SaveSongDto;
import com.feple.feple_backend.artist.song.dto.SongRequestResponseDto;
import com.feple.feple_backend.artist.song.dto.SongResponseDto;
import com.feple.feple_backend.artist.song.dto.SubmitSongRequestDto;
import com.feple.feple_backend.artist.song.dto.YoutubeVideoDto;
import com.feple.feple_backend.artist.song.entity.SongRequest;
import com.feple.feple_backend.artist.song.entity.SongRequestStatus;
import com.feple.feple_backend.artist.song.event.SongRequestApprovedEvent;
import com.feple.feple_backend.artist.song.event.SongRequestRejectedEvent;
import com.feple.feple_backend.artist.song.repository.SongRequestRepository;
import com.feple.feple_backend.global.UserNicknameLookup;
import com.feple.feple_backend.global.exception.ConflictException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SongRequestServiceImplTest {

    @Mock SongRequestRepository songRequestRepository;
    @Mock ArtistRepository artistRepository;
    @Mock UserNicknameLookup nicknameResolver;
    @Mock YoutubeSearchService youtubeSearchService;
    @Mock SongAdminService songAdminService;
    @Mock ApplicationEventPublisher eventPublisher;

    private SongRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SongRequestServiceImpl(
                songRequestRepository, artistRepository, nicknameResolver, youtubeSearchService, songAdminService, eventPublisher);
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
        verify(songRequestRepository, never()).save(any());
    }

    @Test
    void 요청_존재하지_않는_아티스트면_예외() {
        given(artistRepository.findById(99L)).willReturn(Optional.empty());
        SubmitSongRequestDto dto = new SubmitSongRequestDto();
        ReflectionTestUtils.setField(dto, "songTitle", "어떤 곡");

        assertThatThrownBy(() -> service.submit(99L, 10L, dto))
                .isInstanceOf(NoSuchElementException.class);
        verify(songRequestRepository, never()).save(any());
    }

    // ── approveAndMaybeSaveSong ──────────────────────────────────────────

    @Test
    void 승인_유튜브URL_없으면_곡_저장_안함() {
        Artist artist = artist(1L, "아이유");
        SongRequest request = pending(1L, artist, 10L, "밤편지");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));

        boolean saved = service.approveAndMaybeSaveSong(1L, null);

        assertThat(saved).isFalse();
        assertThat(request.getStatus()).isEqualTo(SongRequestStatus.APPROVED);
        verify(songAdminService, never()).saveSongIfAbsent(any(), any());
        verify(eventPublisher).publishEvent(any(SongRequestApprovedEvent.class));
    }

    @Test
    void 승인_유튜브URL_공백이면_곡_저장_안함() {
        Artist artist = artist(1L, "아이유");
        SongRequest request = pending(1L, artist, 10L, "밤편지");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));

        boolean saved = service.approveAndMaybeSaveSong(1L, "  ");

        assertThat(saved).isFalse();
        verify(songAdminService, never()).saveSongIfAbsent(any(), any());
    }

    @Test
    void 승인_유튜브_영상_조회안되면_곡_저장_안함() {
        Artist artist = artist(1L, "아이유");
        SongRequest request = pending(1L, artist, 10L, "밤편지");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));
        given(youtubeSearchService.fetchVideoByUrl("https://youtu.be/abc123")).willReturn(Optional.empty());

        boolean saved = service.approveAndMaybeSaveSong(1L, "https://youtu.be/abc123");

        assertThat(saved).isFalse();
        verify(songAdminService, never()).saveSongIfAbsent(any(), any());
    }

    @Test
    void 승인_유튜브URL_있고_중복곡_아니면_곡_저장() {
        Artist artist = artist(1L, "아이유");
        SongRequest request = pending(1L, artist, 10L, "밤편지");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));
        YoutubeVideoDto video = YoutubeVideoDto.builder()
                .videoId("abc123").title("밤편지 MV").thumbnailUrl("https://thumb").build();
        given(youtubeSearchService.fetchVideoByUrl("https://youtu.be/abc123")).willReturn(Optional.of(video));
        given(songAdminService.saveSongIfAbsent(eq(1L), any(SaveSongDto.class)))
                .willReturn(Optional.of(SongResponseDto.builder().build()));

        boolean saved = service.approveAndMaybeSaveSong(1L, "https://youtu.be/abc123");

        assertThat(saved).isTrue();
        assertThat(request.getYoutubeUrl()).isEqualTo("https://youtu.be/abc123");
        verify(songAdminService).saveSongIfAbsent(eq(1L), any(SaveSongDto.class));
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
        given(songAdminService.saveSongIfAbsent(eq(1L), any(SaveSongDto.class))).willReturn(Optional.empty());

        boolean saved = service.approveAndMaybeSaveSong(1L, "https://youtu.be/abc123");

        assertThat(saved).isFalse();
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

    // ── getPendingCount / getTotalCount ──────────────────────────────────

    @Test
    void getPendingCount_레포지토리에_위임됨() {
        given(songRequestRepository.countByStatus(SongRequestStatus.PENDING)).willReturn(3L);

        assertThat(service.getPendingCount()).isEqualTo(3L);
    }

    @Test
    void getTotalCount_레포지토리에_위임됨() {
        given(songRequestRepository.count()).willReturn(20L);

        assertThat(service.getTotalCount()).isEqualTo(20L);
    }

    // ── getMyAllRequests / getMyRequests ─────────────────────────────────

    @Test
    void 내_전체_요청목록_조회() {
        Artist artist = artist(1L, "아이유");
        SongRequest request = pending(1L, artist, 10L, "밤편지");
        given(nicknameResolver.lookup(10L)).willReturn("닉네임");
        given(songRequestRepository.findByUserIdOrderByCreatedAtDesc(10L)).willReturn(List.of(request));

        List<SongRequestResponseDto> result = service.getMyAllRequests(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSongTitle()).isEqualTo("밤편지");
    }

    @Test
    void 특정_아티스트에_대한_내_요청목록_조회() {
        Artist artist = artist(1L, "아이유");
        SongRequest request = pending(1L, artist, 10L, "밤편지");
        given(nicknameResolver.lookup(10L)).willReturn("닉네임");
        given(songRequestRepository.findByArtistIdAndUserIdOrderByCreatedAtDesc(1L, 10L)).willReturn(List.of(request));

        List<SongRequestResponseDto> result = service.getMyRequests(1L, 10L);

        assertThat(result).hasSize(1);
    }

    // ── getRequestsPage ──────────────────────────────────────────────────

    @Test
    void 상태_ALL이면_필터없이_전체조회() {
        given(songRequestRepository.findWithFilters(eq(null), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));
        given(nicknameResolver.buildMap(anyList(), any())).willReturn(Map.of());

        Page<SongRequestResponseDto> result = service.getRequestsPage(0, 20, "ALL", null);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void 유효한_상태값이면_해당_상태로_필터링() {
        given(songRequestRepository.findWithFilters(eq(SongRequestStatus.APPROVED), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));
        given(nicknameResolver.buildMap(anyList(), any())).willReturn(Map.of());

        service.getRequestsPage(0, 20, "APPROVED", null);

        verify(songRequestRepository).findWithFilters(eq(SongRequestStatus.APPROVED), any(), any(Pageable.class));
    }

    @Test
    void 알수없는_상태값이면_전체조회로_폴백() {
        given(songRequestRepository.findWithFilters(eq(null), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));
        given(nicknameResolver.buildMap(anyList(), any())).willReturn(Map.of());

        service.getRequestsPage(0, 20, "INVALID_STATUS", null);

        verify(songRequestRepository).findWithFilters(eq(null), any(), any(Pageable.class));
    }

    @Test
    void 닉네임_매핑에_없는_사용자는_UNKNOWN으로_표시() {
        Artist artist = artist(1L, "아이유");
        SongRequest request = pending(1L, artist, 10L, "밤편지");
        given(songRequestRepository.findWithFilters(eq(null), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(request)));
        given(nicknameResolver.buildMap(anyList(), any())).willReturn(Map.of());

        Page<SongRequestResponseDto> result = service.getRequestsPage(0, 20, null, null);

        assertThat(result.getContent().get(0).getUserNickname()).isEqualTo(UserNicknameLookup.UNKNOWN);
    }

    // ── getPendingRequests / getPendingPreview ────────────────────────────

    @Test
    void 대기중인_요청목록_조회() {
        Artist artist = artist(1L, "아이유");
        SongRequest request = pending(1L, artist, 10L, "밤편지");
        given(songRequestRepository.findByArtistIdAndStatusOrderByCreatedAtDesc(1L, SongRequestStatus.PENDING))
                .willReturn(List.of(request));
        given(nicknameResolver.buildMap(anyList(), any())).willReturn(Map.of(10L, "닉네임"));

        List<SongRequestResponseDto> result = service.getPendingRequests(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserNickname()).isEqualTo("닉네임");
    }

    @Test
    void 대기중인_요청_미리보기_조회() {
        Artist artist = artist(1L, "아이유");
        SongRequest request = pending(1L, artist, 10L, "밤편지");
        given(songRequestRepository.findByStatusOrderByCreatedAtDesc(SongRequestStatus.PENDING, PageRequest.of(0, 5)))
                .willReturn(List.of(request));

        List<SongRequest> result = service.getPendingPreview(5);

        assertThat(result).hasSize(1);
    }

    // ── reject ───────────────────────────────────────────────────────────

    @Test
    void 반려시_상태변경후_이벤트_발행() {
        Artist artist = artist(1L, "아이유");
        SongRequest request = pending(1L, artist, 10L, "밤편지");
        given(songRequestRepository.findById(1L)).willReturn(Optional.of(request));

        service.reject(1L, "저작권 문제");

        assertThat(request.getStatus()).isEqualTo(SongRequestStatus.REJECTED);
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
