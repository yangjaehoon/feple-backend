package com.feple.feple_backend.artist.suggestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import com.feple.feple_backend.artist.suggestion.dto.SubmitArtistSuggestionDto;
import com.feple.feple_backend.artist.suggestion.entity.ArtistSuggestion;
import com.feple.feple_backend.artist.suggestion.entity.ArtistSuggestionStatus;
import com.feple.feple_backend.artist.suggestion.event.ArtistSuggestionProcessedEvent;
import com.feple.feple_backend.artist.suggestion.repository.ArtistSuggestionRepository;
import com.feple.feple_backend.global.UserNicknameLookup;
import com.feple.feple_backend.global.exception.ConflictException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ArtistSuggestionServiceTest {

    @Mock ArtistSuggestionRepository suggestionRepository;
    @Mock UserNicknameLookup nicknameResolver;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks ArtistSuggestionServiceImpl suggestionService;

    private SubmitArtistSuggestionDto dto(String artistName) {
        SubmitArtistSuggestionDto dto = new SubmitArtistSuggestionDto();
        dto.setArtistName(artistName);
        dto.setNote("꼭 추가해주세요");
        return dto;
    }

    private ArtistSuggestion savedSuggestion(Long id, Long userId, String artistName) {
        return ArtistSuggestion.builder()
                .id(id).userId(userId).artistName(artistName)
                .status(ArtistSuggestionStatus.PENDING).build();
    }

    // ── submit ────────────────────────────────────────────────────────

    @Test
    void 이미_신청한_아티스트_재신청시_ConflictException() {
        given(suggestionRepository.existsByUserIdAndArtistNameIgnoreCaseAndStatus(
                1L, "아이유", ArtistSuggestionStatus.PENDING)).willReturn(true);

        assertThatThrownBy(() -> suggestionService.submit(1L, dto("아이유")))
                .isInstanceOf(ConflictException.class);

        verify(suggestionRepository, never()).save(any());
    }

    @Test
    void 신규_아티스트_신청시_저장됨() {
        given(suggestionRepository.existsByUserIdAndArtistNameIgnoreCaseAndStatus(
                1L, "뉴진스", ArtistSuggestionStatus.PENDING)).willReturn(false);
        given(nicknameResolver.lookup(1L)).willReturn("user1");
        ArtistSuggestion saved = savedSuggestion(10L, 1L, "뉴진스");
        given(suggestionRepository.save(any(ArtistSuggestion.class))).willReturn(saved);

        ArtistSuggestionResponseDto result = suggestionService.submit(1L, dto("뉴진스"));

        assertThat(result.getArtistName()).isEqualTo("뉴진스");
        assertThat(result.getStatus()).isEqualTo("PENDING");
        verify(suggestionRepository).save(any(ArtistSuggestion.class));
    }

    @Test
    void 신청_저장_후_닉네임_조회됨() {
        given(suggestionRepository.existsByUserIdAndArtistNameIgnoreCaseAndStatus(
                2L, "BTS", ArtistSuggestionStatus.PENDING)).willReturn(false);
        given(nicknameResolver.lookup(2L)).willReturn("user2");
        given(suggestionRepository.save(any(ArtistSuggestion.class)))
                .willReturn(savedSuggestion(11L, 2L, "BTS"));

        ArtistSuggestionResponseDto result = suggestionService.submit(2L, dto("BTS"));

        assertThat(result.getUserNickname()).isEqualTo("user2");
    }

    // ── dismiss ───────────────────────────────────────────────────────

    @Test
    void 신청_기각시_상태가_DISMISSED로_변경됨() {
        ArtistSuggestion suggestion = savedSuggestion(1L, 1L, "아이유");
        given(suggestionRepository.findById(1L)).willReturn(Optional.of(suggestion));

        suggestionService.dismiss(1L, "이미 등록된 아티스트입니다.");

        assertThat(suggestion.getStatus()).isEqualTo(ArtistSuggestionStatus.DISMISSED);
        assertThat(suggestion.getProcessNote()).isEqualTo("이미 등록된 아티스트입니다.");
    }

    @Test
    void 존재하지_않는_신청_기각시_예외() {
        given(suggestionRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> suggestionService.dismiss(99L, "사유"))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void 이미_처리된_신청_재기각시_예외() {
        ArtistSuggestion suggestion = ArtistSuggestion.builder()
                .id(1L).userId(1L).artistName("아이유")
                .status(ArtistSuggestionStatus.DISMISSED).build();
        given(suggestionRepository.findById(1L)).willReturn(Optional.of(suggestion));

        assertThatThrownBy(() -> suggestionService.dismiss(1L, "사유"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 처리된 아티스트 신청입니다.");
    }

    @Test
    void 이미_처리된_신청_재승인시_예외() {
        ArtistSuggestion suggestion = ArtistSuggestion.builder()
                .id(1L).userId(1L).artistName("아이유")
                .status(ArtistSuggestionStatus.APPROVED).build();
        given(suggestionRepository.findById(1L)).willReturn(Optional.of(suggestion));

        assertThatThrownBy(() -> suggestionService.approve(1L, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 처리된 아티스트 신청입니다.");
    }

    // ── getPendingCount ──────────────────────────────────────────────────

    @Test
    void getPendingCount_레포지토리에_위임됨() {
        given(suggestionRepository.countByStatus(ArtistSuggestionStatus.PENDING)).willReturn(5L);

        assertThat(suggestionService.getPendingCount()).isEqualTo(5L);
    }

    @Test
    void getProcessedCount_레포지토리에_위임됨() {
        given(suggestionRepository.countByStatus(ArtistSuggestionStatus.DISMISSED)).willReturn(3L);

        assertThat(suggestionService.getProcessedCount()).isEqualTo(3L);
    }

    // ── 관리자 조회 ──────────────────────────────────────────────────────

    @Test
    void 대기중_신청_페이지_조회시_닉네임_매핑() {
        ArtistSuggestion s = savedSuggestion(1L, 1L, "아이유");
        Page<ArtistSuggestion> page = new PageImpl<>(List.of(s));
        given(suggestionRepository.findByStatusOrderByCreatedAtDesc(
                        ArtistSuggestionStatus.PENDING, PageRequest.of(0, 10)))
                .willReturn(page);
        given(nicknameResolver.buildMap(any(List.class), any())).willReturn(Map.of(1L, "user1"));

        Page<ArtistSuggestionResponseDto> result = suggestionService.getSuggestionsPage(0, 10);

        assertThat(result.getContent()).extracting(ArtistSuggestionResponseDto::getUserNickname)
                .containsExactly("user1");
    }

    @Test
    void 처리완료_신청_미리보기는_DISMISSED_상태만_조회() {
        ArtistSuggestion s = savedSuggestion(1L, 1L, "아이유");
        Page<ArtistSuggestion> page = new PageImpl<>(List.of(s));
        given(suggestionRepository.findByStatusOrderByCreatedAtDesc(
                        ArtistSuggestionStatus.DISMISSED, PageRequest.of(0, 5)))
                .willReturn(page);
        given(nicknameResolver.buildMap(any(List.class), any())).willReturn(Map.of());

        List<ArtistSuggestionResponseDto> result = suggestionService.getProcessedSuggestionsPreview(5);

        assertThat(result.get(0).getUserNickname()).isEqualTo(UserNicknameLookup.UNKNOWN);
    }

    @Test
    void 대기중_신청_미리보기는_PENDING_상태만_조회() {
        ArtistSuggestion s = savedSuggestion(1L, 1L, "아이유");
        Page<ArtistSuggestion> page = new PageImpl<>(List.of(s));
        given(suggestionRepository.findByStatusOrderByCreatedAtDesc(
                        ArtistSuggestionStatus.PENDING, PageRequest.of(0, 5)))
                .willReturn(page);
        given(nicknameResolver.buildMap(any(List.class), any())).willReturn(Map.of(1L, "user1"));

        List<ArtistSuggestionResponseDto> result = suggestionService.getPendingSuggestionsPreview(5);

        assertThat(result).hasSize(1);
    }

    // ── approve ──────────────────────────────────────────────────────────

    @Test
    void 승인시_상태변경후_이벤트_발행() {
        ArtistSuggestion suggestion = savedSuggestion(1L, 10L, "아이유");
        given(suggestionRepository.findById(1L)).willReturn(Optional.of(suggestion));

        suggestionService.approve(1L, 100L);

        assertThat(suggestion.isPending()).isFalse();
        ArgumentCaptor<ArtistSuggestionProcessedEvent> captor = ArgumentCaptor.forClass(ArtistSuggestionProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(10L);
        assertThat(captor.getValue().artistId()).isEqualTo(100L);
        assertThat(captor.getValue().artistName()).isEqualTo("아이유");
    }

    @Test
    void 승인시_존재하지_않는_신청이면_예외() {
        given(suggestionRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> suggestionService.approve(1L, 100L))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    // ── dismiss 이벤트 발행 ──────────────────────────────────────────────

    @Test
    void 반려시_이벤트_발행() {
        ArtistSuggestion suggestion = savedSuggestion(1L, 10L, "아이유");
        given(suggestionRepository.findById(1L)).willReturn(Optional.of(suggestion));

        suggestionService.dismiss(1L, "중복 신청");

        ArgumentCaptor<ArtistSuggestionProcessedEvent> captor = ArgumentCaptor.forClass(ArtistSuggestionProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().note()).isEqualTo("중복 신청");
        assertThat(captor.getValue().artistId()).isNull();
    }

    // ── removeAllByUser ──────────────────────────────────────────────────

    @Test
    void 회원탈퇴시_전체_신청_삭제() {
        suggestionService.removeAllByUser(1L);

        verify(suggestionRepository).deleteByUserId(1L);
    }
}
