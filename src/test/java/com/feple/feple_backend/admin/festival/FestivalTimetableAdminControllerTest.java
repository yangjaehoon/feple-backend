package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.timetable.service.TimetableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FestivalTimetableAdminControllerTest {

    @Mock TimetableService timetableService;
    @Mock AdminLogService adminLogService;

    @InjectMocks FestivalTimetableAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── POST /admin/festivals/{festivalId}/timetable ──────────────────────────

    @Test
    void 타임테이블_항목_추가_검증실패_errorMessage_설정() throws Exception {
        // @NotNull festivalDate 없음
        mockMvc.perform(post("/admin/festivals/1/timetable")
                        .param("startTime", "10:00:00")
                        .param("endTime", "11:00:00"))
                .andExpect(redirectedUrl("/admin/festivals/1#timetable"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void 타임테이블_항목_추가_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/timetable")
                        .param("festivalDate", "2026-06-01")
                        .param("startTime", "10:00:00")
                        .param("endTime", "11:00:00"))
                .andExpect(redirectedUrl("/admin/festivals/1#timetable"))
                .andExpect(flash().attribute("successMessage", "타임테이블 항목이 추가되었습니다."));
    }

    @Test
    void 타임테이블_항목_추가_서비스_예외_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(timetableService).createEntry(anyLong(), any());

        mockMvc.perform(post("/admin/festivals/1/timetable")
                        .param("festivalDate", "2026-06-01")
                        .param("startTime", "10:00:00")
                        .param("endTime", "11:00:00"))
                .andExpect(flash().attribute("errorMessage", "항목 추가 중 오류가 발생했습니다."));
    }

    // ── POST /admin/festivals/{festivalId}/timetable/{entryId}/update ─────────

    @Test
    void 타임테이블_항목_수정_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/timetable/3/update")
                        .param("festivalDate", "2026-06-01")
                        .param("startTime", "14:00:00")
                        .param("endTime", "15:00:00"))
                .andExpect(redirectedUrl("/admin/festivals/1#timetable"))
                .andExpect(flash().attribute("successMessage", "타임테이블 항목이 수정되었습니다."));

        then(timetableService).should().updateEntry(eq(1L), eq(3L), any());
    }

    @Test
    void 타임테이블_항목_수정_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(timetableService).updateEntry(anyLong(), anyLong(), any());

        mockMvc.perform(post("/admin/festivals/1/timetable/3/update")
                        .param("festivalDate", "2026-06-01")
                        .param("startTime", "14:00:00")
                        .param("endTime", "15:00:00"))
                .andExpect(flash().attribute("errorMessage", "항목 수정 중 오류가 발생했습니다."));
    }

    // ── POST /admin/festivals/{festivalId}/timetable/{entryId}/delete ─────────

    @Test
    void 타임테이블_항목_삭제_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/timetable/3/delete"))
                .andExpect(redirectedUrl("/admin/festivals/1#timetable"))
                .andExpect(flash().attribute("successMessage", "항목이 삭제되었습니다."));

        then(timetableService).should().deleteEntry(1L, 3L);
    }

    @Test
    void 타임테이블_항목_삭제_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(timetableService).deleteEntry(anyLong(), anyLong());

        mockMvc.perform(post("/admin/festivals/1/timetable/3/delete"))
                .andExpect(flash().attribute("errorMessage", "항목 삭제에 실패했습니다."));
    }
}
