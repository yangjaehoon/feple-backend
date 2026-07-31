package com.feple.feple_backend.admin.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UnmatchedArtistSuggestionServiceTest {

    @Mock UnmatchedArtistSuggestionRepository repository;

    @InjectMocks UnmatchedArtistSuggestionService service;

    // ── saveAll ───────────────────────────────────────────────────────────────

    @Test
    void saveAll_새_이름은_엔티티_생성_후_저장() {
        given(repository.incrementMentionCountByNameIgnoreCase("신인가수")).willReturn(0);

        service.saveAll(List.of("신인가수"));

        verify(repository).save(any(UnmatchedArtistSuggestion.class));
    }

    @Test
    void saveAll_이미_있는_이름은_원자적_UPDATE만_수행_저장_없음() {
        given(repository.incrementMentionCountByNameIgnoreCase("신인가수")).willReturn(1);

        service.saveAll(List.of("신인가수"));

        verify(repository).incrementMentionCountByNameIgnoreCase("신인가수");
        verify(repository, never()).save(any());
    }

    @Test
    void saveAll_빈_이름은_건너뜀() {
        service.saveAll(List.of("  ", ""));

        verify(repository, never()).incrementMentionCountByNameIgnoreCase(any());
        verify(repository, never()).save(any());
    }

    @Test
    void saveAll_빈_리스트는_아무_동작_안함() {
        service.saveAll(List.of());

        verifyNoInteractions(repository);
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    void getAll_repository_조회_후_DTO_변환() {
        UnmatchedArtistSuggestion s = mock(UnmatchedArtistSuggestion.class);
        given(s.getId()).willReturn(1L);
        given(s.getName()).willReturn("신인가수");
        given(s.getMentionCount()).willReturn(3);
        given(repository.findAllOrderByMentionCountDesc()).willReturn(List.of(s));

        List<UnmatchedArtistSuggestionDto> result = service.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("신인가수");
        assertThat(result.get(0).mentionCount()).isEqualTo(3);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_repository_deleteById_위임() {
        service.delete(42L);

        verify(repository).deleteById(42L);
    }
}
