package com.feple.feple_backend.festival.suggestion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.festival.suggestion.dto.FestivalSuggestionResponseDto;
import com.feple.feple_backend.festival.suggestion.dto.SubmitFestivalSuggestionDto;
import com.feple.feple_backend.festival.suggestion.entity.FestivalSuggestion;
import com.feple.feple_backend.festival.suggestion.entity.FestivalSuggestionStatus;
import com.feple.feple_backend.festival.suggestion.event.FestivalSuggestionProcessedEvent;
import com.feple.feple_backend.festival.suggestion.repository.FestivalSuggestionRepository;
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

@ExtendWith(MockitoExtension.class)
class FestivalSuggestionServiceImplTest {

    @Mock FestivalSuggestionRepository suggestionRepository;
    @Mock UserNicknameLookup nicknameResolver;
    @Mock ApplicationEventPublisher eventPublisher;

    private FestivalSuggestionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FestivalSuggestionServiceImpl(suggestionRepository, nicknameResolver, eventPublisher);
    }

    private FestivalSuggestion pending(Long id, Long userId, String festivalName) {
        FestivalSuggestion s = FestivalSuggestion.builder()
                .id(id).userId(userId).festivalName(festivalName).status(FestivalSuggestionStatus.PENDING).build();
        return s;
    }

    // ── submit ───────────────────────────────────────────────────────────

    @Test
    void 신청_정상_제출() {
        SubmitFestivalSuggestionDto dto = new SubmitFestivalSuggestionDto();
        dto.setFestivalName("뮤직페스티벌");
        dto.setNote("기대돼요");
        given(suggestionRepository.existsByUserIdAndFestivalNameIgnoreCaseAndStatus(
                10L, "뮤직페스티벌", FestivalSuggestionStatus.PENDING)).willReturn(false);
        FestivalSuggestion saved = pending(1L, 10L, "뮤직페스티벌");
        given(suggestionRepository.save(any(FestivalSuggestion.class))).willReturn(saved);
        given(nicknameResolver.lookup(10L)).willReturn("닉네임");

        FestivalSuggestionResponseDto result = service.submit(10L, dto);

        assertThat(result.getFestivalName()).isEqualTo("뮤직페스티벌");
        assertThat(result.getUserNickname()).isEqualTo("닉네임");
    }

    @Test
    void 신청_이미_동일_페스티벌_신청중이면_예외() {
        SubmitFestivalSuggestionDto dto = new SubmitFestivalSuggestionDto();
        dto.setFestivalName("뮤직페스티벌");
        given(suggestionRepository.existsByUserIdAndFestivalNameIgnoreCaseAndStatus(
                10L, "뮤직페스티벌", FestivalSuggestionStatus.PENDING)).willReturn(true);

        assertThatThrownBy(() -> service.submit(10L, dto))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("이미 신청한");
    }

    // ── 관리자 조회 ──────────────────────────────────────────────────────

    @Test
    void 대기중_신청_페이지_조회시_닉네임_매핑() {
        FestivalSuggestion s = pending(1L, 10L, "A페스티벌");
        Page<FestivalSuggestion> page = new PageImpl<>(List.of(s));
        given(suggestionRepository.findByStatusOrderByCreatedAtDesc(
                FestivalSuggestionStatus.PENDING, PageRequest.of(0, 10))).willReturn(page);
        given(nicknameResolver.buildMap(any(List.class), any())).willReturn(Map.of(10L, "닉네임"));

        Page<FestivalSuggestionResponseDto> result = service.getSuggestionsPage(0, 10);

        assertThat(result.getContent()).extracting(FestivalSuggestionResponseDto::getUserNickname)
                .containsExactly("닉네임");
    }

    @Test
    void 대기중_신청_페이지_조회시_닉네임_없으면_알수없음() {
        FestivalSuggestion s = pending(1L, 10L, "A페스티벌");
        Page<FestivalSuggestion> page = new PageImpl<>(List.of(s));
        given(suggestionRepository.findByStatusOrderByCreatedAtDesc(
                FestivalSuggestionStatus.PENDING, PageRequest.of(0, 10))).willReturn(page);
        given(nicknameResolver.buildMap(any(List.class), any())).willReturn(Map.of());

        Page<FestivalSuggestionResponseDto> result = service.getSuggestionsPage(0, 10);

        assertThat(result.getContent().get(0).getUserNickname()).isEqualTo(UserNicknameLookup.UNKNOWN);
    }

    @Test
    void 처리완료_신청_미리보기는_DISMISSED_상태만_조회() {
        FestivalSuggestion s = pending(1L, 10L, "A페스티벌");
        Page<FestivalSuggestion> page = new PageImpl<>(List.of(s));
        given(suggestionRepository.findByStatusOrderByCreatedAtDesc(
                FestivalSuggestionStatus.DISMISSED, PageRequest.of(0, 5))).willReturn(page);
        given(nicknameResolver.buildMap(any(List.class), any())).willReturn(Map.of(10L, "닉네임"));

        List<FestivalSuggestionResponseDto> result = service.getProcessedSuggestionsPreview(5);

        assertThat(result).hasSize(1);
    }

    @Test
    void 대기중_신청_미리보기는_PENDING_상태만_조회() {
        FestivalSuggestion s = pending(1L, 10L, "A페스티벌");
        Page<FestivalSuggestion> page = new PageImpl<>(List.of(s));
        given(suggestionRepository.findByStatusOrderByCreatedAtDesc(
                FestivalSuggestionStatus.PENDING, PageRequest.of(0, 5))).willReturn(page);
        given(nicknameResolver.buildMap(any(List.class), any())).willReturn(Map.of(10L, "닉네임"));

        List<FestivalSuggestionResponseDto> result = service.getPendingSuggestionsPreview(5);

        assertThat(result).hasSize(1);
    }

    @Test
    void 대기중_신청_카운트_위임() {
        given(suggestionRepository.countByStatus(FestivalSuggestionStatus.PENDING)).willReturn(3L);

        assertThat(service.getPendingCount()).isEqualTo(3L);
    }

    @Test
    void 처리완료_신청_카운트_위임() {
        given(suggestionRepository.countByStatus(FestivalSuggestionStatus.DISMISSED)).willReturn(7L);

        assertThat(service.getProcessedCount()).isEqualTo(7L);
    }

    // ── approve ──────────────────────────────────────────────────────────

    @Test
    void 승인시_상태변경후_이벤트_발행() {
        FestivalSuggestion s = pending(1L, 10L, "A페스티벌");
        given(suggestionRepository.findById(1L)).willReturn(Optional.of(s));

        service.approve(1L, 100L);

        assertThat(s.isPending()).isFalse();
        ArgumentCaptor<FestivalSuggestionProcessedEvent> captor = ArgumentCaptor.forClass(FestivalSuggestionProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(10L);
        assertThat(captor.getValue().festivalId()).isEqualTo(100L);
        assertThat(captor.getValue().festivalName()).isEqualTo("A페스티벌");
    }

    @Test
    void 승인시_존재하지_않는_신청이면_예외() {
        given(suggestionRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(1L, 100L)).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 승인시_이미_처리된_신청이면_예외() {
        FestivalSuggestion s = pending(1L, 10L, "A페스티벌");
        s.approve(999L);
        given(suggestionRepository.findById(1L)).willReturn(Optional.of(s));

        assertThatThrownBy(() -> service.approve(1L, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 처리된");
    }

    // ── dismiss ──────────────────────────────────────────────────────────

    @Test
    void 반려시_상태변경후_이벤트_발행() {
        FestivalSuggestion s = pending(1L, 10L, "A페스티벌");
        given(suggestionRepository.findById(1L)).willReturn(Optional.of(s));

        service.dismiss(1L, "장소 미정");

        assertThat(s.isPending()).isFalse();
        ArgumentCaptor<FestivalSuggestionProcessedEvent> captor = ArgumentCaptor.forClass(FestivalSuggestionProcessedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().note()).isEqualTo("장소 미정");
        assertThat(captor.getValue().festivalId()).isNull();
    }

    @Test
    void 반려시_이미_처리된_신청이면_예외() {
        FestivalSuggestion s = pending(1L, 10L, "A페스티벌");
        s.dismiss("이미반려");
        given(suggestionRepository.findById(1L)).willReturn(Optional.of(s));

        assertThatThrownBy(() -> service.dismiss(1L, "다시반려"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 처리된");
    }

    // ── removeAllByUser ──────────────────────────────────────────────────

    @Test
    void 회원탈퇴시_전체_신청_삭제() {
        service.removeAllByUser(10L);

        verify(suggestionRepository).deleteByUserId(10L);
    }
}
