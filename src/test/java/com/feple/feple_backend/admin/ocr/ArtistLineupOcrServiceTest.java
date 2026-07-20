package com.feple.feple_backend.admin.ocr;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.global.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArtistLineupOcrServiceTest {

    @Mock GeminiOcrClient geminiOcrClient;
    @Mock ArtistRepository artistRepository;
    @Mock ArtistFestivalService artistFestivalService;
    @Mock UnmatchedArtistSuggestionService suggestionService;

    @InjectMocks ArtistLineupOcrService ocrService;

    // ── matchArtist (parseArtistLineup 내부) ─────────────────────────────────

    // 인메모리 매칭이 이름 하나에서 조기 성립하면 nameEn/aliases stub이 안 쓰일 수 있어 lenient 처리
    private static Artist mockArtist(Long id, String name, String nameEn) {
        Artist artist = mock(Artist.class);
        lenient().when(artist.getId()).thenReturn(id);
        lenient().when(artist.getName()).thenReturn(name);
        lenient().when(artist.getNameEn()).thenReturn(nameEn);
        lenient().when(artist.getAliases()).thenReturn(List.of());
        return artist;
    }

    @Test
    void parseArtistLineup_정확히_일치하는_아티스트_있으면_ID_반환() throws Exception {
        Artist artist = mockArtist(10L, "아이유", null);
        given(geminiOcrClient.parseLineup(any())).willReturn(new OcrParseResult<>(List.of(new LineupRawResult("아이유", 95)), false));
        given(artistRepository.findAllWithAliases()).willReturn(List.of(artist));

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null).entries();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).artistId()).isEqualTo(10L);
        assertThat(results.get(0).matchedName()).isEqualTo("아이유");
    }

    @Test
    void parseArtistLineup_정확_매칭_없고_부분_매칭이_1개면_해당_아티스트_반환() throws Exception {
        // nameEn이 "IU"와 완전히 같지 않고 포함만 하므로 exact가 아닌 partial로만 매칭된다
        Artist artist = mockArtist(20L, "아이유", "IU (아이유)");
        given(geminiOcrClient.parseLineup(any())).willReturn(new OcrParseResult<>(List.of(new LineupRawResult("IU", 80)), false));
        given(artistRepository.findAllWithAliases()).willReturn(List.of(artist));

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null).entries();

        assertThat(results.get(0).artistId()).isEqualTo(20L);
    }

    @Test
    void parseArtistLineup_부분_매칭이_복수면_artistId_null_반환() throws Exception {
        Artist a1 = mockArtist(1L, "아이유", "IU (아이유)");
        Artist a2 = mockArtist(2L, "IU Fan Club", null);
        given(geminiOcrClient.parseLineup(any())).willReturn(new OcrParseResult<>(List.of(new LineupRawResult("IU", 70)), false));
        given(artistRepository.findAllWithAliases()).willReturn(List.of(a1, a2));

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null).entries();

        assertThat(results.get(0).artistId()).isNull();
        assertThat(results.get(0).matchedName()).isNull();
    }

    @Test
    void parseArtistLineup_매칭_없으면_artistId_null_반환() throws Exception {
        given(geminiOcrClient.parseLineup(any())).willReturn(new OcrParseResult<>(List.of(new LineupRawResult("존재안함", 50)), false));
        given(artistRepository.findAllWithAliases()).willReturn(List.of());

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null).entries();

        assertThat(results.get(0).artistId()).isNull();
        assertThat(results.get(0).parsedName()).isEqualTo("존재안함");
    }

    @Test
    void parseArtistLineup_confidence가_null이면_0으로_처리() throws Exception {
        Artist artist = mockArtist(1L, "아이유", null);
        given(geminiOcrClient.parseLineup(any())).willReturn(new OcrParseResult<>(List.of(new LineupRawResult("아이유", null)), false));
        given(artistRepository.findAllWithAliases()).willReturn(List.of(artist));

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null).entries();

        assertThat(results.get(0).confidence()).isEqualTo(0);
    }

    @Test
    void parseArtistLineup_geminiOcrClient가_truncated_true면_그대로_전파() throws Exception {
        given(geminiOcrClient.parseLineup(any()))
                .willReturn(new OcrParseResult<>(List.of(new LineupRawResult("아이유", 95)), true));
        given(artistRepository.findAllWithAliases()).willReturn(List.of());

        OcrParseResult<ArtistLineupOcrResult> result = ocrService.parseArtistLineup(null);

        assertThat(result.truncated()).isTrue();
    }

    // ── applyArtistLineup ─────────────────────────────────────────────────────

    @Test
    void applyArtistLineup_모두_성공시_added_카운트_일치() {
        ocrService.applyArtistLineup(new LineupOcrApplyRequestDto(1L, List.of(10L, 20L), null));

        verify(artistFestivalService, times(2)).addArtistToFestival(eq(1L), any());
    }

    @Test
    void applyArtistLineup_중복_아티스트는_ConflictException_처리후_duplicates_카운트() {
        willThrow(new ConflictException("중복")).given(artistFestivalService).addArtistToFestival(eq(1L), any());

        LineupApplyResult result = ocrService.applyArtistLineup(new LineupOcrApplyRequestDto(1L, List.of(10L, 20L), null));

        assertThat(result.added()).isEqualTo(0);
        assertThat(result.duplicates()).isEqualTo(2);
    }

    @Test
    void applyArtistLineup_일부_중복시_added와_duplicates_합계가_requested와_일치() {
        given(artistFestivalService.addArtistToFestival(eq(1L), argThat(r -> r.getArtistId().equals(10L)))).willReturn(10L);
        willThrow(new ConflictException("중복")).given(artistFestivalService).addArtistToFestival(eq(1L), argThat(r -> r.getArtistId().equals(20L)));

        LineupApplyResult result = ocrService.applyArtistLineup(new LineupOcrApplyRequestDto(1L, List.of(10L, 20L), null));

        assertThat(result.requested()).isEqualTo(2);
        assertThat(result.added()).isEqualTo(1);
        assertThat(result.duplicates()).isEqualTo(1);
    }

    @Test
    void applyArtistLineup_미매칭_이름은_suggestionService에_위임() {
        List<String> unmatched = List.of("신인가수");

        ocrService.applyArtistLineup(new LineupOcrApplyRequestDto(1L, List.of(), unmatched));

        verify(suggestionService).saveAll(unmatched);
    }

    @Test
    void applyArtistLineup_unmatchedNames_null이면_suggestionService_호출_안됨() {
        ocrService.applyArtistLineup(new LineupOcrApplyRequestDto(1L, List.of(), null));

        verify(suggestionService, never()).saveAll(any());
    }

    // ── getSuggestions / deleteSuggestion ────────────────────────────────────

    @Test
    void getSuggestions_suggestionService_getAll_위임() {
        given(suggestionService.getAll()).willReturn(List.of());

        ocrService.getSuggestions();

        verify(suggestionService).getAll();
    }

    @Test
    void deleteSuggestion_suggestionService_delete_위임() {
        ocrService.deleteSuggestion(42L);

        verify(suggestionService).delete(42L);
    }

    // ── geminiOcrClient 위임 메서드 ───────────────────────────────────────────

    @Test
    void isConfigured_geminiOcrClient_위임() {
        given(geminiOcrClient.isConfigured()).willReturn(true);

        assertThat(ocrService.isConfigured()).isTrue();
    }
}
