package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.checklist.FestivalChecklistService;
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
class FestivalChecklistAdminControllerTest {

    @Mock FestivalChecklistService festivalChecklistService;

    @InjectMocks FestivalChecklistAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── POST /admin/festivals/{id}/checklist ──────────────────────────────────

    @Test
    void 체크리스트_토글_성공_checked값_반환() throws Exception {
        given(festivalChecklistService.isChecked(1L, "poster")).willReturn(true);

        mockMvc.perform(post("/admin/festivals/1/checklist")
                        .param("field", "poster"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checked").value(true));

        then(festivalChecklistService).should().toggle(1L, "poster");
    }

    @Test
    void 체크리스트_토글_IllegalArgument_400_반환() throws Exception {
        willThrow(new IllegalArgumentException("알 수 없는 필드"))
                .given(festivalChecklistService).toggle(anyLong(), anyString());

        mockMvc.perform(post("/admin/festivals/1/checklist")
                        .param("field", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("알 수 없는 필드"));
    }

    // ── POST /admin/festivals/{id}/checklist/memo ─────────────────────────────

    @Test
    void 메모_저장_성공_200_반환() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/checklist/memo")
                        .param("memo", "준비 완료"))
                .andExpect(status().isOk());

        then(festivalChecklistService).should().saveMemo(1L, "준비 완료");
    }
}
