package com.feple.feple_backend.admin.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        given(geminiOcrClient.parseLineup(any(), any())).willReturn(new OcrParseResult<>(List.of(new LineupRawResult("아이유", 95, null)), false));
        given(artistRepository.findAllWithAliases()).willReturn(List.of(artist));

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null, null).entries();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).artistId()).isEqualTo(10L);
        assertThat(results.get(0).matchedName()).isEqualTo("아이유");
    }

    @Test
    void parseArtistLineup_정확_매칭_없고_부분_매칭이_1개면_해당_아티스트_반환() throws Exception {
        // nameEn이 "IU"와 완전히 같지 않고 포함만 하므로 exact가 아닌 partial로만 매칭된다
        Artist artist = mockArtist(20L, "아이유", "IU (아이유)");
        given(geminiOcrClient.parseLineup(any(), any())).willReturn(new OcrParseResult<>(List.of(new LineupRawResult("IU", 80, null)), false));
        given(artistRepository.findAllWithAliases()).willReturn(List.of(artist));

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null, null).entries();

        assertThat(results.get(0).artistId()).isEqualTo(20L);
    }

    @Test
    void parseArtistLineup_OCR값이_영문한글_조합이고_DB값이_단일이어도_매칭() throws Exception {
        // OCR이 "LOCO (로꼬)"처럼 DB에 저장된 단일 이름(로꼬/Loco)보다 긴 조합 문자열을
        // 반환하는 경우로, candidate.contains(name) 방향만 확인하면 매칭에 실패했었다
        Artist artist = mockArtist(30L, "로꼬", "Loco");
        given(geminiOcrClient.parseLineup(any(), any())).willReturn(new OcrParseResult<>(List.of(new LineupRawResult("LOCO (로꼬)", 100, null)), false));
        given(artistRepository.findAllWithAliases()).willReturn(List.of(artist));

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null, null).entries();

        assertThat(results.get(0).artistId()).isEqualTo(30L);
    }

    @Test
    void parseArtistLineup_부분_매칭이_복수면_artistId_null_반환() throws Exception {
        Artist a1 = mockArtist(1L, "아이유", "IU (아이유)");
        Artist a2 = mockArtist(2L, "IU Fan Club", null);
        given(geminiOcrClient.parseLineup(any(), any())).willReturn(new OcrParseResult<>(List.of(new LineupRawResult("IU", 70, null)), false));
        given(artistRepository.findAllWithAliases()).willReturn(List.of(a1, a2));

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null, null).entries();

        assertThat(results.get(0).artistId()).isNull();
        assertThat(results.get(0).matchedName()).isNull();
    }

    @Test
    void parseArtistLineup_매칭_없으면_artistId_null_반환() throws Exception {
        given(geminiOcrClient.parseLineup(any(), any())).willReturn(new OcrParseResult<>(List.of(new LineupRawResult("존재안함", 50, null)), false));
        given(artistRepository.findAllWithAliases()).willReturn(List.of());

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null, null).entries();

        assertThat(results.get(0).artistId()).isNull();
        assertThat(results.get(0).parsedName()).isEqualTo("존재안함");
    }

    @Test
    void parseArtistLineup_confidence가_null이면_0으로_처리() throws Exception {
        Artist artist = mockArtist(1L, "아이유", null);
        given(geminiOcrClient.parseLineup(any(), any())).willReturn(new OcrParseResult<>(List.of(new LineupRawResult("아이유", null, null)), false));
        given(artistRepository.findAllWithAliases()).willReturn(List.of(artist));

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null, null).entries();

        assertThat(results.get(0).confidence()).isEqualTo(0);
    }

    @Test
    void parseArtistLineup_OCR_이름이_null이어도_NPE없이_미매칭_처리() throws Exception {
        // Gemini가 이름을 인식하지 못해 name=null을 반환하는 항목이 있어도 전체 요청이 죽으면 안 됨
        Artist artist = mockArtist(1L, "아이유", "IU");
        given(geminiOcrClient.parseLineup(any(), any()))
                .willReturn(new OcrParseResult<>(List.of(new LineupRawResult(null, 40, null)), false));
        given(artistRepository.findAllWithAliases()).willReturn(List.of(artist));

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null, null).entries();

        assertThat(results.get(0).artistId()).isNull();
    }

    @Test
    void parseArtistLineup_geminiOcrClient가_truncated_true면_그대로_전파() throws Exception {
        given(geminiOcrClient.parseLineup(any(), any()))
                .willReturn(new OcrParseResult<>(List.of(new LineupRawResult("아이유", 95, null)), true));
        given(artistRepository.findAllWithAliases()).willReturn(List.of());

        OcrParseResult<ArtistLineupOcrResult> result = ocrService.parseArtistLineup(null, null);

        assertThat(result.truncated()).isTrue();
    }

    @Test
    void parseArtistLineup_OCR이_추출한_date가_결과에_그대로_반영() throws Exception {
        Artist artist = mockArtist(10L, "아이유", null);
        given(geminiOcrClient.parseLineup(any(), any()))
                .willReturn(new OcrParseResult<>(List.of(new LineupRawResult("아이유", 95, "2026-08-01")), false));
        given(artistRepository.findAllWithAliases()).willReturn(List.of(artist));

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null, null).entries();

        assertThat(results.get(0).date()).isEqualTo("2026-08-01");
    }

    @Test
    void parseArtistLineup_year파라미터가_geminiOcrClient에_그대로_전달() throws Exception {
        given(geminiOcrClient.parseLineup(any(), eq(2026)))
                .willReturn(new OcrParseResult<>(List.of(), false));
        given(artistRepository.findAllWithAliases()).willReturn(List.of());

        ocrService.parseArtistLineup(null, 2026);

        verify(geminiOcrClient).parseLineup(any(), eq(2026));
    }

    // ── applyArtistLineup ─────────────────────────────────────────────────────

    // applyArtistLineup은 아티스트마다 개별 조회/등록을 반복하지 않고 linkArtistsToFestival로
    // 일괄 위임한다(N+1 방지) — added/duplicates/errors 집계 자체는 linkArtistsToFestival의
    // 책임이라 여기서는 그 결과가 LineupApplyResult로 그대로 전달되는지만 검증한다.
    @Test
    void applyArtistLineup_linkArtistsToFestival에_아티스트ID_목록_그대로_위임() {
        given(artistFestivalService.linkArtistsToFestival(eq(1L), eq(List.of(10L, 20L)), anyMap()))
                .willReturn(new ArtistFestivalService.LinkArtistsResult(2, 0, 0));

        ocrService.applyArtistLineup(new LineupOcrApplyRequestDto(1L,
                List.of(new LineupOcrArtistEntry(10L, null), new LineupOcrArtistEntry(20L, null)), null));

        verify(artistFestivalService).linkArtistsToFestival(eq(1L), eq(List.of(10L, 20L)), anyMap());
    }

    @Test
    void applyArtistLineup_날짜가_있는_항목은_artistId별_날짜_맵으로_전달() {
        given(artistFestivalService.linkArtistsToFestival(eq(1L), any(), any()))
                .willReturn(new ArtistFestivalService.LinkArtistsResult(2, 0, 0));

        ocrService.applyArtistLineup(new LineupOcrApplyRequestDto(1L,
                List.of(new LineupOcrArtistEntry(10L, "2026-08-01"), new LineupOcrArtistEntry(20L, null)), null));

        verify(artistFestivalService).linkArtistsToFestival(
                eq(1L), eq(List.of(10L, 20L)), eq(Map.of(10L, LocalDate.of(2026, 8, 1))));
    }

    @Test
    void applyArtistLineup_날짜_형식이_잘못되면_무시하고_나머지는_그대로_진행() {
        given(artistFestivalService.linkArtistsToFestival(eq(1L), any(), any()))
                .willReturn(new ArtistFestivalService.LinkArtistsResult(1, 0, 0));

        ocrService.applyArtistLineup(new LineupOcrApplyRequestDto(1L,
                List.of(new LineupOcrArtistEntry(10L, "잘못된날짜")), null));

        verify(artistFestivalService).linkArtistsToFestival(eq(1L), eq(List.of(10L)), eq(Map.of()));
    }

    @Test
    void applyArtistLineup_모두_중복이면_duplicates_카운트_그대로_전달() {
        given(artistFestivalService.linkArtistsToFestival(eq(1L), any(), any()))
                .willReturn(new ArtistFestivalService.LinkArtistsResult(0, 2, 0));

        LineupApplyResult result = ocrService.applyArtistLineup(new LineupOcrApplyRequestDto(1L,
                List.of(new LineupOcrArtistEntry(10L, null), new LineupOcrArtistEntry(20L, null)), null));

        assertThat(result.added()).isEqualTo(0);
        assertThat(result.duplicates()).isEqualTo(2);
    }

    @Test
    void applyArtistLineup_일부_중복시_added와_duplicates_합계가_requested와_일치() {
        given(artistFestivalService.linkArtistsToFestival(eq(1L), any(), any()))
                .willReturn(new ArtistFestivalService.LinkArtistsResult(1, 1, 0));

        LineupApplyResult result = ocrService.applyArtistLineup(new LineupOcrApplyRequestDto(1L,
                List.of(new LineupOcrArtistEntry(10L, null), new LineupOcrArtistEntry(20L, null)), null));

        assertThat(result.requested()).isEqualTo(2);
        assertThat(result.added()).isEqualTo(1);
        assertThat(result.duplicates()).isEqualTo(1);
    }

    @Test
    void applyArtistLineup_존재하지_않는_아티스트는_failed로_집계() {
        given(artistFestivalService.linkArtistsToFestival(eq(1L), any(), any()))
                .willReturn(new ArtistFestivalService.LinkArtistsResult(1, 0, 1));

        LineupApplyResult result = ocrService.applyArtistLineup(new LineupOcrApplyRequestDto(1L,
                List.of(new LineupOcrArtistEntry(10L, null), new LineupOcrArtistEntry(20L, null)), null));

        assertThat(result.requested()).isEqualTo(2);
        assertThat(result.added()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
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
