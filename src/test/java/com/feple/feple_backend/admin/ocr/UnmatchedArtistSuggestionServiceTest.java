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

    // 신규/기존 이름 구분과 동시 언급 경합은 모두 ON DUPLICATE KEY UPDATE 업서트 한 문장이
    // DB에서 처리한다. "UPDATE 후 0건이면 INSERT" 2단계와 그 제약 위반 catch는 제거됐다 —
    // 예외가 프록시 밖으로 나온 시점에 트랜잭션이 이미 rollback-only라 복구가 불가능했다.
    @Test
    void saveAll_이름마다_업서트_한_번씩_호출() {
        service.saveAll(List.of("신인가수", "기존가수"));

        verify(repository).upsertMentionCount("신인가수");
        verify(repository).upsertMentionCount("기존가수");
        verify(repository, never()).save(any());
    }

    @Test
    void saveAll_이름_앞뒤_공백은_제거하고_업서트() {
        service.saveAll(List.of("  신인가수  "));

        verify(repository).upsertMentionCount("신인가수");
    }

    @Test
    void saveAll_빈_이름은_건너뜀() {
        service.saveAll(List.of("  ", ""));

        verify(repository, never()).upsertMentionCount(any());
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
