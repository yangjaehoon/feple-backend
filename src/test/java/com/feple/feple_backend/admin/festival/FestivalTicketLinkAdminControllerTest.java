package com.feple.feple_backend.admin.festival;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.ticketlink.service.FestivalTicketLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FestivalTicketLinkAdminControllerTest {

    @Mock FestivalTicketLinkService ticketLinkService;
    @Mock AdminLogService adminLogService;

    @InjectMocks FestivalTicketLinkAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── POST /admin/festivals/{festivalId}/ticket-links ────────────────────────

    @Test
    void 링크_생성_URL없으면_errorMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/ticket-links")
                        .param("label", "인터파크"))
                .andExpect(redirectedUrl("/admin/festivals/1#ticket-links"))
                .andExpect(flash().attributeExists("errorMessage"));

        then(ticketLinkService).should(never()).createTicketLink(anyLong(), any());
    }

    @Test
    void 링크_생성_URL_형식오류시_errorMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/ticket-links")
                        .param("url", "not-a-url"))
                .andExpect(redirectedUrl("/admin/festivals/1#ticket-links"))
                .andExpect(flash().attributeExists("errorMessage"));

        then(ticketLinkService).should(never()).createTicketLink(anyLong(), any());
    }

    @Test
    void 링크_생성_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/ticket-links")
                        .param("label", "인터파크")
                        .param("url", "https://tickets.interpark.com/example"))
                .andExpect(redirectedUrl("/admin/festivals/1#ticket-links"))
                .andExpect(flash().attribute("successMessage", "예매 링크가 추가되었습니다."));

        then(ticketLinkService).should().createTicketLink(eq(1L), any());
    }

    @Test
    void 링크_생성_서비스_예외_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(ticketLinkService).createTicketLink(anyLong(), any());

        mockMvc.perform(post("/admin/festivals/1/ticket-links")
                        .param("url", "https://tickets.interpark.com/example"))
                .andExpect(flash().attribute("errorMessage", "예매 링크 추가에 실패했습니다."));
    }

    // ── POST /admin/festivals/{festivalId}/ticket-links/{ticketLinkId}/delete ──

    @Test
    void 링크_삭제_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/ticket-links/5/delete"))
                .andExpect(redirectedUrl("/admin/festivals/1#ticket-links"))
                .andExpect(flash().attribute("successMessage", "예매 링크가 삭제되었습니다."));

        then(ticketLinkService).should().deleteTicketLink(1L, 5L);
    }

    @Test
    void 링크_삭제_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(ticketLinkService).deleteTicketLink(anyLong(), anyLong());

        mockMvc.perform(post("/admin/festivals/1/ticket-links/5/delete"))
                .andExpect(flash().attribute("errorMessage", "예매 링크 삭제에 실패했습니다."));
    }
}
