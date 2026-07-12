package com.feple.feple_backend.admin.ocr;

import com.feple.feple_backend.timetable.service.TimetableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TimetableOcrServiceTest {

    @Mock GeminiOcrClient geminiOcrClient;
    @Mock TimetableService timetableService;

    @InjectMocks TimetableOcrService ocrService;

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

    // ── geminiOcrClient 위임 메서드 ───────────────────────────────────────────

    @Test
    void isConfigured_geminiOcrClient_위임() {
        given(geminiOcrClient.isConfigured()).willReturn(true);

        assertThat(ocrService.isConfigured()).isTrue();
    }
}
