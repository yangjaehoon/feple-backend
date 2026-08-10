package com.feple.feple_backend.admin.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.timetable.service.TimetableService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimetableOcrServiceTest {

    @Mock GeminiOcrClient geminiOcrClient;
    @Mock TimetableService timetableService;
    @Mock Festival festival;

    @InjectMocks TimetableOcrService ocrService;

    @BeforeEach
    void setUp() {
        // applyEntries가 루프 시작 전 festival을 한 번만 조회하도록 바뀌어(N+1 방지),
        // 모든 테스트가 이 스텁을 거친다.
        given(timetableService.getFestivalOrThrow(anyLong())).willReturn(festival);
    }

    // ── applyEntries ──────────────────────────────────────────────────────────

    @Test
    void applyEntries_유효한_엔트리는_타임테이블에_저장되고_savedCount_증가() {
        TimetableOcrResultDto entry = new TimetableOcrResultDto("아이유", "Main", "2024-07-20", "18:00", "19:00", 95, null);
        TimetableOcrApplyRequestDto req = new TimetableOcrApplyRequestDto(1L, List.of(entry));

        TimetableOcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.savedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(0);
        verify(timetableService).createEntry(eq(festival), any());
    }

    @Test
    void applyEntries_날짜_누락_엔트리는_실패_처리되고_저장_안됨() {
        TimetableOcrResultDto entry = new TimetableOcrResultDto("아이유", "Main", null, "18:00", "19:00", 95, null);
        TimetableOcrApplyRequestDto req = new TimetableOcrApplyRequestDto(1L, List.of(entry));

        TimetableOcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.savedCount()).isEqualTo(0);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.failures().get(0).reason()).isEqualTo("날짜 누락");
        verify(timetableService, never()).createEntry(any(Festival.class), any());
    }

    @Test
    void applyEntries_날짜_빈_문자열_엔트리는_실패_처리() {
        TimetableOcrResultDto entry = new TimetableOcrResultDto("아이유", "Main", "  ", "18:00", "19:00", 95, null);
        TimetableOcrApplyRequestDto req = new TimetableOcrApplyRequestDto(1L, List.of(entry));

        TimetableOcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.failures().get(0).reason()).isEqualTo("날짜 누락");
    }

    @Test
    void applyEntries_시간_누락_엔트리는_실패_처리() {
        TimetableOcrResultDto entry = new TimetableOcrResultDto("아이유", "Main", "2024-07-20", null, null, 95, null);
        TimetableOcrApplyRequestDto req = new TimetableOcrApplyRequestDto(1L, List.of(entry));

        TimetableOcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.failures().get(0).reason()).isEqualTo("시작/종료 시간 누락");
    }

    @Test
    void applyEntries_날짜_형식_오류_엔트리는_실패_처리() {
        TimetableOcrResultDto entry = new TimetableOcrResultDto("아이유", "Main", "20240720", "18:00", "19:00", 95, null);
        TimetableOcrApplyRequestDto req = new TimetableOcrApplyRequestDto(1L, List.of(entry));

        TimetableOcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.failures().get(0).reason()).startsWith("날짜 형식 오류");
    }

    @Test
    void applyEntries_시간_형식_오류_엔트리는_실패_처리() {
        TimetableOcrResultDto entry = new TimetableOcrResultDto("아이유", "Main", "2024-07-20", "18시", "19:00", 95, null);
        TimetableOcrApplyRequestDto req = new TimetableOcrApplyRequestDto(1L, List.of(entry));

        TimetableOcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.failures().get(0).reason()).startsWith("시간 형식 오류");
        verify(timetableService, never()).createEntry(any(Festival.class), any());
    }

    @Test
    void applyEntries_timetableService_예외시_실패_목록에_추가() {
        TimetableOcrResultDto entry = new TimetableOcrResultDto("아이유", "Main", "2024-07-20", "18:00", "19:00", 95, null);
        TimetableOcrApplyRequestDto req = new TimetableOcrApplyRequestDto(1L, List.of(entry));
        willThrow(new IllegalArgumentException("스테이지 없음")).given(timetableService).createEntry(any(Festival.class), any());

        TimetableOcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.savedCount()).isEqualTo(0);
        assertThat(result.failedCount()).isEqualTo(1);
        assertThat(result.failures().get(0).reason()).isEqualTo("스테이지 없음");
    }

    @Test
    void applyEntries_유효_실패_혼합시_각각_집계() {
        TimetableOcrResultDto valid = new TimetableOcrResultDto("아이유", "Main", "2024-07-20", "18:00", "19:00", 95, null);
        TimetableOcrResultDto invalid = new TimetableOcrResultDto("BTS", "Sub", null, "20:00", "21:00", 80, null);
        TimetableOcrApplyRequestDto req = new TimetableOcrApplyRequestDto(1L, List.of(valid, invalid));

        TimetableOcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.savedCount()).isEqualTo(1);
        assertThat(result.failedCount()).isEqualTo(1);
    }

    @Test
    void applyEntries_OPS_타입_엔트리는_스테이지명이_방송기호로_설정됨() {
        TimetableOcrResultDto entry = new TimetableOcrResultDto("MC", "Main", "2024-07-20", "12:00", "12:30", 90, "OPS");
        TimetableOcrApplyRequestDto req = new TimetableOcrApplyRequestDto(1L, List.of(entry));

        ocrService.applyEntries(req);

        var captor = ArgumentCaptor.forClass(com.feple.feple_backend.timetable.dto.TimetableEntryRequestDto.class);
        verify(timetableService).createEntry(eq(festival), captor.capture());
        assertThat(captor.getValue().getStageName()).isEqualTo("📢");
    }

    @Test
    void applyEntries_실패_엔트리에_인덱스와_아티스트명_포함() {
        TimetableOcrResultDto entry = new TimetableOcrResultDto("아이유", "Main", null, "18:00", "19:00", 95, null);
        TimetableOcrApplyRequestDto req = new TimetableOcrApplyRequestDto(1L, List.of(entry));

        TimetableOcrApplyResultDto result = ocrService.applyEntries(req);

        TimetableOcrFailure failure = result.failures().get(0);
        assertThat(failure.index()).isEqualTo(0);
        assertThat(failure.artist()).isEqualTo("아이유");
    }

    @Test
    void applyEntries_아티스트명_null이면_실패_맵에_대시_표시() {
        TimetableOcrResultDto entry = new TimetableOcrResultDto(null, null, null, "18:00", "19:00", 95, null);
        TimetableOcrApplyRequestDto req = new TimetableOcrApplyRequestDto(1L, List.of(entry));

        TimetableOcrApplyResultDto result = ocrService.applyEntries(req);

        assertThat(result.failures().get(0).artist()).isEqualTo("—");
    }

    // ── geminiOcrClient 위임 메서드 ───────────────────────────────────────────

    @Test
    void isConfigured_geminiOcrClient_위임() {
        given(geminiOcrClient.isConfigured()).willReturn(true);

        assertThat(ocrService.isConfigured()).isTrue();
    }
}
