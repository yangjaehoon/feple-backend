package com.feple.feple_backend.admin.notice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.push.AdminPushService;
import com.feple.feple_backend.notice.service.NoticeAdminService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class NoticeAdminControllerTest {

    @Mock NoticeAdminService noticeAdminService;
    @Mock AdminLogService adminLogService;
    @Mock AdminPushService adminPushService;

    @InjectMocks NoticeAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String... roles) {
        var authorities = List.of(roles).stream().map(SimpleGrantedAuthority::new).toList();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("admin", null, authorities));
    }

    // ── POST /admin/notices/new — 알림 발송 권한/성공/실패 분기 ────────────────

    @Test
    void 알림_미요청시_전체푸시_호출_없음() throws Exception {
        given(noticeAdminService.createNotice(any())).willReturn(1L);

        mockMvc.perform(post("/admin/notices/new")
                        .param("title", "제목")
                        .param("content", "내용"))
                .andExpect(flash().attribute("successMessage", "공지사항이 등록되었습니다."));

        then(adminPushService).should(never()).sendToAll(anyString(), anyString());
    }

    @Test
    void SUPER_ADMIN이_알림_요청시_전체푸시_발송() throws Exception {
        authenticateAs("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
        given(noticeAdminService.createNotice(any())).willReturn(1L);

        mockMvc.perform(post("/admin/notices/new")
                        .param("title", "제목")
                        .param("content", "내용")
                        .param("sendNotification", "true"))
                .andExpect(flash().attribute("successMessage", "공지사항이 등록되었습니다."));

        then(adminPushService).should().sendToAll("제목", "내용");
    }

    @Test
    void SUPER_ADMIN_아닌_관리자가_알림_요청해도_무시됨() throws Exception {
        authenticateAs("ROLE_ADMIN");
        given(noticeAdminService.createNotice(any())).willReturn(1L);

        mockMvc.perform(post("/admin/notices/new")
                        .param("title", "제목")
                        .param("content", "내용")
                        .param("sendNotification", "true"))
                .andExpect(flash().attribute("successMessage", "공지사항이 등록되었습니다."));

        then(adminPushService).should(never()).sendToAll(anyString(), anyString());
    }

    @Test
    void 알림_발송_실패해도_공지_등록은_성공으로_처리() throws Exception {
        authenticateAs("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
        given(noticeAdminService.createNotice(any())).willReturn(1L);
        willThrow(new IllegalArgumentException("등록된 디바이스 토큰이 없습니다."))
                .given(adminPushService).sendToAll(anyString(), anyString());

        mockMvc.perform(post("/admin/notices/new")
                        .param("title", "제목")
                        .param("content", "내용")
                        .param("sendNotification", "true"))
                .andExpect(flash().attribute("successMessage", "공지사항이 등록되었습니다. (알림 발송에는 실패했습니다)"));

        then(noticeAdminService).should().createNotice(any());
    }

    @Test
    void 알림_제목과_본문은_푸시_길이_제한에_맞게_잘려서_발송() throws Exception {
        authenticateAs("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
        given(noticeAdminService.createNotice(any())).willReturn(1L);
        String longTitle = "가".repeat(150);
        String longContent = "나".repeat(600);

        mockMvc.perform(post("/admin/notices/new")
                        .param("title", longTitle)
                        .param("content", longContent)
                        .param("sendNotification", "true"))
                .andExpect(flash().attributeExists("successMessage"));

        then(adminPushService).should().sendToAll(
                eq("가".repeat(99) + "…"),
                eq("나".repeat(499) + "…"));
    }
}
