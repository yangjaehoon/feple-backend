package com.feple.feple_backend.admin.ocr;

import com.feple.feple_backend.artist.entity.Artist;
import com.feple.feple_backend.artist.repository.ArtistRepository;
import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.timetable.service.TimetableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OcrServiceTest {

    @Mock GeminiOcrClient geminiOcrClient;
    @Mock TimetableService timetableService;
    @Mock ArtistRepository artistRepository;
    @Mock ArtistFestivalService artistFestivalService;
    @Mock UnmatchedArtistSuggestionService suggestionService;

    @InjectMocks OcrService ocrService;

    // ── applyEntries ──────────────────────────────────────────────────────────

    @Test
    void applyEntries_유효한_엔트리는_타임테이블에_저장되고_savedCount_증가() {
        OcrResultDto entry = new OcrResultDto("아이유", "Main", "2024-07-20", "18:00", "19:00", 95, null);
        OcrApplyRequestDto req = new OcrApplyRequestDto(1L, List.of(entry));

        OcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.savedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(0);
        verify(timetableService).createEntry(eq(1L), any());
    }

    @Test
    void applyEntries_날짜_누락_엔트리는_실패_처리되고_저장_안됨() {
        OcrResultDto entry = new OcrResultDto("아이유", "Main", null, "18:00", "19:00", 95, null);
        OcrApplyRequestDto req = new OcrApplyRequestDto(1L, List.of(entry));

        OcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.savedCount()).isEqualTo(0);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.failures().get(0).get("reason")).isEqualTo("날짜 누락");
        verify(timetableService, never()).createEntry(anyLong(), any());
    }

    @Test
    void applyEntries_날짜_빈_문자열_엔트리는_실패_처리() {
        OcrResultDto entry = new OcrResultDto("아이유", "Main", "  ", "18:00", "19:00", 95, null);
        OcrApplyRequestDto req = new OcrApplyRequestDto(1L, List.of(entry));

        OcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.failures().get(0).get("reason")).isEqualTo("날짜 누락");
    }

    @Test
    void applyEntries_시간_누락_엔트리는_실패_처리() {
        OcrResultDto entry = new OcrResultDto("아이유", "Main", "2024-07-20", null, null, 95, null);
        OcrApplyRequestDto req = new OcrApplyRequestDto(1L, List.of(entry));

        OcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.failures().get(0).get("reason")).isEqualTo("시작/종료 시간 누락");
    }

    @Test
    void applyEntries_날짜_형식_오류_엔트리는_실패_처리() {
        OcrResultDto entry = new OcrResultDto("아이유", "Main", "20240720", "18:00", "19:00", 95, null);
        OcrApplyRequestDto req = new OcrApplyRequestDto(1L, List.of(entry));

        OcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.failures().get(0).get("reason")).startsWith("날짜 형식 오류");
    }

    @Test
    void applyEntries_timetableService_예외시_실패_목록에_추가() {
        OcrResultDto entry = new OcrResultDto("아이유", "Main", "2024-07-20", "18:00", "19:00", 95, null);
        OcrApplyRequestDto req = new OcrApplyRequestDto(1L, List.of(entry));
        willThrow(new IllegalArgumentException("스테이지 없음")).given(timetableService).createEntry(anyLong(), any());

        OcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.savedCount()).isEqualTo(0);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.failures().get(0).get("reason")).isEqualTo("스테이지 없음");
    }

    @Test
    void applyEntries_유효_실패_혼합시_각각_집계() {
        OcrResultDto valid = new OcrResultDto("아이유", "Main", "2024-07-20", "18:00", "19:00", 95, null);
        OcrResultDto invalid = new OcrResultDto("BTS", "Sub", null, "20:00", "21:00", 80, null);
        OcrApplyRequestDto req = new OcrApplyRequestDto(1L, List.of(valid, invalid));

        OcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.savedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
    }

    @Test
    void applyEntries_OPS_타입_엔트리는_스테이지명이_방송기호로_설정됨() {
        OcrResultDto entry = new OcrResultDto("MC", "Main", "2024-07-20", "12:00", "12:30", 90, "OPS");
        OcrApplyRequestDto req = new OcrApplyRequestDto(1L, List.of(entry));

        ocrService.applyEntries(req);

        var captor = ArgumentCaptor.forClass(com.feple.feple_backend.timetable.dto.TimetableEntryRequestDto.class);
        verify(timetableService).createEntry(eq(1L), captor.capture());
        assertThat(captor.getValue().getStageName()).isEqualTo("📢");
    }

    @Test
    void applyEntries_실패_엔트리에_인덱스와_아티스트명_포함() {
        OcrResultDto entry = new OcrResultDto("아이유", "Main", null, "18:00", "19:00", 95, null);
        OcrApplyRequestDto req = new OcrApplyRequestDto(1L, List.of(entry));

        OcrApplyResultDto result = ocrService.applyEntries(req);

        Map<String, String> failure = result.failures().get(0);
        assertThat(failure.get("index")).isEqualTo("0");
        assertThat(failure.get("artist")).isEqualTo("아이유");
    }

    @Test
    void applyEntries_아티스트명_null이면_실패_맵에_대시_표시() {
        OcrResultDto entry = new OcrResultDto(null, null, null, "18:00", "19:00", 95, null);
        OcrApplyRequestDto req = new OcrApplyRequestDto(1L, List.of(entry));

        OcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.failures().get(0).get("artist")).isEqualTo("—");
    }

    // ── matchArtist (parseArtistLineup 내부) ─────────────────────────────────

    @Test
    void parseArtistLineup_정확히_일치하는_아티스트_있으면_ID_반환() throws Exception {
        Artist artist = mock(Artist.class);
        given(artist.getId()).willReturn(10L);
        given(artist.getName()).willReturn("아이유");
        given(geminiOcrClient.parseLineup(any())).willReturn(new OcrParseResult<>(List.of(new LineupRawResult("아이유", 95)), false));
        given(artistRepository.findExactByNameIgnoreCase("아이유")).willReturn(Optional.of(artist));

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null).entries();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).artistId()).isEqualTo(10L);
        assertThat(results.get(0).matchedName()).isEqualTo("아이유");
    }

    @Test
    void parseArtistLineup_정확_매칭_없고_부분_매칭이_1개면_해당_아티스트_반환() throws Exception {
        Artist artist = mock(Artist.class);
        given(artist.getId()).willReturn(20L);
        given(artist.getName()).willReturn("아이유");
        given(geminiOcrClient.parseLineup(any())).willReturn(new OcrParseResult<>(List.of(new LineupRawResult("IU", 80)), false));
        given(artistRepository.findExactByNameIgnoreCase("IU")).willReturn(Optional.empty());
        given(artistRepository.findByNameOrNameEnContainingIgnoreCase("IU")).willReturn(List.of(artist));

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null).entries();

        assertThat(results.get(0).artistId()).isEqualTo(20L);
    }

    @Test
    void parseArtistLineup_부분_매칭이_복수면_artistId_null_반환() throws Exception {
        Artist a1 = mock(Artist.class);
        Artist a2 = mock(Artist.class);
        given(geminiOcrClient.parseLineup(any())).willReturn(new OcrParseResult<>(List.of(new LineupRawResult("IU", 70)), false));
        given(artistRepository.findExactByNameIgnoreCase("IU")).willReturn(Optional.empty());
        given(artistRepository.findByNameOrNameEnContainingIgnoreCase("IU")).willReturn(List.of(a1, a2));

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null).entries();

        assertThat(results.get(0).artistId()).isNull();
        assertThat(results.get(0).matchedName()).isNull();
    }

    @Test
    void parseArtistLineup_매칭_없으면_artistId_null_반환() throws Exception {
        given(geminiOcrClient.parseLineup(any())).willReturn(new OcrParseResult<>(List.of(new LineupRawResult("존재안함", 50)), false));
        given(artistRepository.findExactByNameIgnoreCase("존재안함")).willReturn(Optional.empty());
        given(artistRepository.findByNameOrNameEnContainingIgnoreCase("존재안함")).willReturn(List.of());

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null).entries();

        assertThat(results.get(0).artistId()).isNull();
        assertThat(results.get(0).parsedName()).isEqualTo("존재안함");
    }

    @Test
    void parseArtistLineup_confidence가_null이면_0으로_처리() throws Exception {
        Artist artist = mock(Artist.class);
        given(artist.getId()).willReturn(1L);
        given(artist.getName()).willReturn("아이유");
        given(geminiOcrClient.parseLineup(any())).willReturn(new OcrParseResult<>(List.of(new LineupRawResult("아이유", null)), false));
        given(artistRepository.findExactByNameIgnoreCase("아이유")).willReturn(Optional.of(artist));

        List<ArtistLineupOcrResult> results = ocrService.parseArtistLineup(null).entries();

        assertThat(results.get(0).confidence()).isEqualTo(0);
    }

    @Test
    void parseArtistLineup_geminiOcrClient가_truncated_true면_그대로_전파() throws Exception {
        given(geminiOcrClient.parseLineup(any()))
                .willReturn(new OcrParseResult<>(List.of(new LineupRawResult("아이유", 95)), true));
        given(artistRepository.findExactByNameIgnoreCase("아이유")).willReturn(Optional.empty());
        given(artistRepository.findByNameOrNameEnContainingIgnoreCase("아이유")).willReturn(List.of());

        OcrParseResult<ArtistLineupOcrResult> result = ocrService.parseArtistLineup(null);

        assertThat(result.truncated()).isTrue();
    }

    // ── applyArtistLineup ─────────────────────────────────────────────────────

    @Test
    void applyArtistLineup_모두_성공시_added_카운트_일치() {
        ocrService.applyArtistLineup(1L, List.of(10L, 20L), null);

        verify(artistFestivalService, times(2)).addArtistToFestival(eq(1L), any());
    }

    @Test
    void applyArtistLineup_중복_아티스트는_ConflictException_처리후_duplicates_카운트() {
        willThrow(new ConflictException("중복")).given(artistFestivalService).addArtistToFestival(eq(1L), any());

        LineupApplyResult result = ocrService.applyArtistLineup(1L, List.of(10L, 20L), null);

        assertThat(result.added()).isEqualTo(0);
        assertThat(result.duplicates()).isEqualTo(2);
    }

    @Test
    void applyArtistLineup_일부_중복시_added와_duplicates_합계가_requested와_일치() {
        given(artistFestivalService.addArtistToFestival(eq(1L), argThat(r -> r.getArtistId().equals(10L)))).willReturn(10L);
        willThrow(new ConflictException("중복")).given(artistFestivalService).addArtistToFestival(eq(1L), argThat(r -> r.getArtistId().equals(20L)));

        LineupApplyResult result = ocrService.applyArtistLineup(1L, List.of(10L, 20L), null);

        assertThat(result.requested()).isEqualTo(2);
        assertThat(result.added()).isEqualTo(1);
        assertThat(result.duplicates()).isEqualTo(1);
    }

    @Test
    void applyArtistLineup_미매칭_이름은_suggestionService에_위임() {
        List<String> unmatched = List.of("신인가수");

        ocrService.applyArtistLineup(1L, List.of(), unmatched);

        verify(suggestionService).saveAll(unmatched);
    }

    @Test
    void applyArtistLineup_unmatchedNames_null이면_suggestionService_호출_안됨() {
        ocrService.applyArtistLineup(1L, List.of(), null);

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

    @Test
    void getTodayUsage_geminiOcrClient_위임() {
        given(geminiOcrClient.getTodayUsage()).willReturn(5);

        assertThat(ocrService.getTodayUsage()).isEqualTo(5);
    }

    @Test
    void getDailyLimit_geminiOcrClient_위임() {
        given(geminiOcrClient.getDailyLimit()).willReturn(10);

        assertThat(ocrService.getDailyLimit()).isEqualTo(10);
    }
}
