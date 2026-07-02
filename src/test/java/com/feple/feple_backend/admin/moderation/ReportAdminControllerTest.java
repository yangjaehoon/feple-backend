package com.feple.feple_backend.admin.moderation;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.service.ReportAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReportAdminControllerTest {

    @Mock AdminLogService adminLogService;

    @SuppressWarnings("unchecked")
    ReportAdminService<Object> postHandler = mock(ReportAdminService.class);

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        given(postHandler.getReportType()).willReturn("post");
        ReportAdminController controller = new ReportAdminController(List.of(postHandler), adminLogService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/reports ────────────────────────────────────────────────────

    @Test
    void 목록_조회_뷰와_모델_속성_확인() throws Exception {
        given(postHandler.searchReportsForAdmin(any())).willReturn(new PageImpl<>(List.of()));
        given(postHandler.getPendingCount()).willReturn(2L);
        given(postHandler.getTotalCount()).willReturn(10L);

        mockMvc.perform(get("/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/moderation/reports"))
                .andExpect(model().attributeExists("reports", "pendingCount", "totalCount",
                        "status", "type", "keyword", "authorReportCounts"));
    }

    // ── POST /admin/reports/{id}/delete ───────────────────────────────────────

    @Test
    void 콘텐츠_삭제_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/reports/1/delete")
                        .param("type", "post").param("status", "PENDING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "콘텐츠를 삭제하고 신고를 처리했습니다."));

        then(postHandler).should().deleteContentAndResolve(1L);
    }

    @Test
    void 콘텐츠_삭제_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(postHandler).deleteContentAndResolve(anyLong());

        mockMvc.perform(post("/admin/reports/1/delete")
                        .param("type", "post").param("status", "PENDING"))
                .andExpect(flash().attribute("errorMessage", "삭제 처리 중 오류가 발생했습니다."));
    }

    // ── POST /admin/reports/{id}/dismiss ──────────────────────────────────────

    @Test
    void 신고_기각_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/reports/1/dismiss")
                        .param("type", "post").param("status", "PENDING"))
                .andExpect(flash().attribute("successMessage", "신고를 기각했습니다."));

        then(postHandler).should().dismissReport(1L);
    }

    // ── POST /admin/reports/bulk-dismiss ──────────────────────────────────────

    @Test
    void 일괄_기각_ids_없으면_errorMessage_선택된_항목_없음() throws Exception {
        mockMvc.perform(post("/admin/reports/bulk-dismiss")
                        .param("type", "post").param("status", "PENDING")
                        .param("ids", ""))
                .andExpect(flash().attribute("errorMessage", "선택된 항목이 없습니다."));

        then(postHandler).should(never()).bulkDismiss(any());
    }

    @Test
    void 일괄_기각_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/reports/bulk-dismiss")
                        .param("type", "post").param("status", "PENDING")
                        .param("ids", "1", "2"))
                .andExpect(flash().attribute("successMessage", "2건을 일괄 기각했습니다."));

        then(postHandler).should().bulkDismiss(List.of(1L, 2L));
    }

    // ── POST /admin/reports/bulk-delete ───────────────────────────────────────

    @Test
    void 일괄_삭제_ids_없으면_errorMessage_선택된_항목_없음() throws Exception {
        mockMvc.perform(post("/admin/reports/bulk-delete")
                        .param("type", "post").param("status", "PENDING")
                        .param("ids", ""))
                .andExpect(flash().attribute("errorMessage", "선택된 항목이 없습니다."));
    }

    @Test
    void 일괄_삭제_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/reports/bulk-delete")
                        .param("type", "post").param("status", "PENDING")
                        .param("ids", "1", "2"))
                .andExpect(flash().attribute("successMessage", "2건의 콘텐츠를 삭제하고 신고를 처리했습니다."));
    }
}
