package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.stage.service.StageService;
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
class FestivalStageAdminControllerTest {

    @Mock StageService stageService;
    @Mock AdminLogService adminLogService;

    @InjectMocks FestivalStageAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── POST /admin/festivals/{festivalId}/stages ─────────────────────────────

    @Test
    void 스테이지_추가_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/stages")
                        .param("name", "MAIN STAGE"))
                .andExpect(redirectedUrl("/admin/festivals/1#timetable"))
                .andExpect(flash().attribute("successMessage", "스테이지가 추가되었습니다."));

        then(stageService).should().createStage(1L, "MAIN STAGE");
    }

    @Test
    void 스테이지_추가_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(stageService).createStage(anyLong(), anyString());

        mockMvc.perform(post("/admin/festivals/1/stages")
                        .param("name", "MAIN STAGE"))
                .andExpect(flash().attribute("errorMessage", "스테이지 추가에 실패했습니다."));
    }

    // ── POST /admin/festivals/{festivalId}/stages/{stageId}/delete ────────────

    @Test
    void 스테이지_삭제_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/stages/2/delete"))
                .andExpect(redirectedUrl("/admin/festivals/1#timetable"))
                .andExpect(flash().attribute("successMessage", "스테이지가 삭제되었습니다."));

        then(stageService).should().deleteStage(2L);
    }

    @Test
    void 스테이지_삭제_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(stageService).deleteStage(anyLong());

        mockMvc.perform(post("/admin/festivals/1/stages/2/delete"))
                .andExpect(flash().attribute("errorMessage", "스테이지 삭제에 실패했습니다."));
    }

    // ── POST /admin/festivals/{festivalId}/stages/{stageId}/up ───────────────

    @Test
    void 스테이지_순서_올리기_성공_successMessage_없음() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/stages/2/up"))
                .andExpect(redirectedUrl("/admin/festivals/1#timetable"))
                .andExpect(flash().attributeCount(0));

        then(stageService).should().moveUp(1L, 2L);
    }

    @Test
    void 스테이지_순서_올리기_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(stageService).moveUp(anyLong(), anyLong());

        mockMvc.perform(post("/admin/festivals/1/stages/2/up"))
                .andExpect(flash().attribute("errorMessage", "순서 변경에 실패했습니다."));
    }

    // ── POST /admin/festivals/{festivalId}/stages/{stageId}/down ─────────────

    @Test
    void 스테이지_순서_내리기_성공_successMessage_없음() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/stages/2/down"))
                .andExpect(redirectedUrl("/admin/festivals/1#timetable"))
                .andExpect(flash().attributeCount(0));

        then(stageService).should().moveDown(1L, 2L);
    }
}
