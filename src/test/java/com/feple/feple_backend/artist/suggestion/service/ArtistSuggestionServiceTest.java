package com.feple.feple_backend.artist.suggestion.service;

import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import com.feple.feple_backend.artist.suggestion.dto.SubmitArtistSuggestionDto;
import com.feple.feple_backend.artist.suggestion.entity.ArtistSuggestion;
import com.feple.feple_backend.artist.suggestion.entity.ArtistSuggestionStatus;
import com.feple.feple_backend.artist.suggestion.repository.ArtistSuggestionRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArtistSuggestionServiceTest {

    @Mock ArtistSuggestionRepository suggestionRepository;
    @Mock UserRepository userRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks ArtistSuggestionServiceImpl suggestionService;

    private SubmitArtistSuggestionDto dto(String artistName) {
        SubmitArtistSuggestionDto dto = new SubmitArtistSuggestionDto();
        dto.setArtistName(artistName);
        dto.setNote("꼭 추가해주세요");
        return dto;
    }

    private User user(Long id) {
        return User.builder().id(id).nickname("user" + id)
                .oauthId("o" + id).role(UserRole.USER).build();
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
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
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
        given(userRepository.findById(2L)).willReturn(Optional.of(user(2L)));
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

    // ── countPending ──────────────────────────────────────────────────

    @Test
    void countPending_레포지토리에_위임됨() {
        given(suggestionRepository.countByStatus(ArtistSuggestionStatus.PENDING)).willReturn(5L);

        assertThat(suggestionService.countPending()).isEqualTo(5L);
    }
}
