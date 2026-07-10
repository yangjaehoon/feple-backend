package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.festival.setlistrequest.entity.SetlistChangeRequestStatus;
import com.feple.feple_backend.festival.setlistrequest.service.SetlistChangeRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SetlistRequestAdminControllerTest {

    @Mock SetlistChangeRequestService service;
    @Mock AdminLogService adminLogService;

    @InjectMocks SetlistRequestAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/setlist-requests ───────────────────────────────────────────

    @Test
    void 목록_조회_뷰와_모델_속성_확인() throws Exception {
        given(service.list(eq(SetlistChangeRequestStatus.PENDING), eq(""), any()))
                .willReturn(new PageImpl<>(List.of()));
        given(service.countPending()).willReturn(3L);

        mockMvc.perform(get("/admin/setlist-requests"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/setlist-request/list"))
                .andExpect(model().attributeExists("requests", "status", "keyword", "pendingCount"))
                .andExpect(model().attribute("pendingCount", 3L));
    }

    @Test
    void 유효하지_않은_status는_PENDING으로_fallback() throws Exception {
        given(service.list(eq(SetlistChangeRequestStatus.PENDING), any(), any()))
                .willReturn(new PageImpl<>(List.of()));
        given(service.countPending()).willReturn(0L);

        mockMvc.perform(get("/admin/setlist-requests").param("status", "INVALID"))
                .andExpect(status().isOk());

        then(service).should().list(eq(SetlistChangeRequestStatus.PENDING), any(), any());
    }

    @Test
    void RESOLVED_status_조회() throws Exception {
        given(service.list(eq(SetlistChangeRequestStatus.RESOLVED), any(), any()))
                .willReturn(new PageImpl<>(List.of()));
        given(service.countPending()).willReturn(0L);

        mockMvc.perform(get("/admin/setlist-requests").param("status", "RESOLVED"))
                .andExpect(status().isOk());

        then(service).should().list(eq(SetlistChangeRequestStatus.RESOLVED), any(), any());
    }

    // ── POST /admin/setlist-requests/{id}/resolve ─────────────────────────────

    @Test
    void 처리_성공_successMessage_설정() throws Exception {
        given(service.countByStatus(SetlistChangeRequestStatus.PENDING)).willReturn(5L);

        mockMvc.perform(post("/admin/setlist-requests/1/resolve")
                        .param("status", "PENDING")
                        .param("page", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "처리 완료로 표시했습니다."));

        then(service).should().resolve(1L);
    }

    @Test
    void 처리_성공_리다이렉트_URL에_status와_page_포함() throws Exception {
        given(service.countByStatus(SetlistChangeRequestStatus.PENDING)).willReturn(20L);

        mockMvc.perform(post("/admin/setlist-requests/1/resolve")
                        .param("status", "PENDING")
                        .param("page", "0"))
                .andExpect(redirectedUrl("/admin/setlist-requests?status=PENDING&page=0"));
    }

    @Test
    void 처리_후_남은_항목이_현재_페이지보다_적으면_마지막_유효_페이지로_이동() throws Exception {
        // 남은 5건: maxPage = (5-1)/LIST_PAGE_SIZE(20) = 0 → page=2는 0으로 클램핑
        given(service.countByStatus(SetlistChangeRequestStatus.PENDING)).willReturn(5L);

        mockMvc.perform(post("/admin/setlist-requests/1/resolve")
                        .param("status", "PENDING")
                        .param("page", "2"))
                .andExpect(redirectedUrl("/admin/setlist-requests?status=PENDING&page=0"));
    }

    @Test
    void 처리_성공_keyword_있으면_리다이렉트_URL에_포함() throws Exception {
        given(service.countByStatus(SetlistChangeRequestStatus.PENDING)).willReturn(1L);

        mockMvc.perform(post("/admin/setlist-requests/1/resolve")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("keyword", "라인업"))
                .andExpect(redirectedUrl("/admin/setlist-requests?status=PENDING&page=0&keyword=라인업"));
    }

    @Test
    void 처리_실패_NoSuchElement_메시지_노출() throws Exception {
        willThrow(new NoSuchElementException("존재하지 않는 요청")).given(service).resolve(anyLong());
        given(service.countByStatus(any())).willReturn(0L);

        mockMvc.perform(post("/admin/setlist-requests/99/resolve")
                        .param("status", "PENDING")
                        .param("page", "0"))
                .andExpect(flash().attribute("errorMessage", "존재하지 않는 요청"));
    }

    @Test
    void 처리_실패_RuntimeException_고정_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("DB 연결 오류")).given(service).resolve(anyLong());
        given(service.countByStatus(any())).willReturn(0L);

        mockMvc.perform(post("/admin/setlist-requests/99/resolve")
                        .param("status", "PENDING")
                        .param("page", "0"))
                .andExpect(flash().attribute("errorMessage", "처리 중 오류가 발생했습니다."));
    }
}
