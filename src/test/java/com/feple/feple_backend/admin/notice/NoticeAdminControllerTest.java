package com.feple.feple_backend.admin.notice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.push.AdminPushService;
import com.feple.feple_backend.notice.dto.NoticeResponseDto;
import com.feple.feple_backend.notice.service.NoticeAdminService;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
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
        then(adminLogService).should(never()).log(eq(AdminAction.NOTICE_PUSH), anyString(), any(), anyString());
    }

    @Test
    void SUPER_ADMIN이_알림_요청시_전체푸시_발송하고_감사로그_기록() throws Exception {
        authenticateAs("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
        given(noticeAdminService.createNotice(any())).willReturn(1L);

        mockMvc.perform(post("/admin/notices/new")
                        .param("title", "제목")
                        .param("content", "내용")
                        .param("sendNotification", "true"))
                .andExpect(flash().attribute("successMessage", "공지사항이 등록되었습니다."));

        then(adminPushService).should().sendToAll("제목", "내용");
        then(adminLogService).should().log(AdminAction.NOTICE_PUSH, "NOTICE", 1L, "제목");
    }

    @Test
    void SUPER_ADMIN_아닌_관리자가_알림_요청해도_무시되고_감사로그_없음() throws Exception {
        authenticateAs("ROLE_ADMIN");
        given(noticeAdminService.createNotice(any())).willReturn(1L);

        mockMvc.perform(post("/admin/notices/new")
                        .param("title", "제목")
                        .param("content", "내용")
                        .param("sendNotification", "true"))
                .andExpect(flash().attribute("successMessage", "공지사항이 등록되었습니다."));

        then(adminPushService).should(never()).sendToAll(anyString(), anyString());
        then(adminLogService).should(never()).log(eq(AdminAction.NOTICE_PUSH), anyString(), any(), anyString());
    }

    @Test
    void 알림_발송_실패해도_공지_등록은_성공으로_처리하고_알림_감사로그는_남기지_않음() throws Exception {
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
        then(adminLogService).should(never()).log(eq(AdminAction.NOTICE_PUSH), anyString(), any(), anyString());
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

    @Test
    void 자르는_지점_근처에_공백이_있으면_단어_경계에서_자름() throws Exception {
        authenticateAs("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
        given(noticeAdminService.createNotice(any())).willReturn(1L);
        // 공백이 잘라낼 지점(99번째 글자) 근처(50번째 이후)에 있으면 그 공백에서 끊는다.
        String titleWithLateSpace = "a".repeat(90) + " " + "b".repeat(20);

        mockMvc.perform(post("/admin/notices/new")
                        .param("title", titleWithLateSpace)
                        .param("content", "내용")
                        .param("sendNotification", "true"))
                .andExpect(flash().attributeExists("successMessage"));

        then(adminPushService).should().sendToAll(eq("a".repeat(90) + "…"), anyString());
    }

    @Test
    void 자르는_지점에서_너무_먼_공백은_무시하고_글자수_기준으로_자름() throws Exception {
        authenticateAs("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
        given(noticeAdminService.createNotice(any())).willReturn(1L);
        // 공백이 자를 지점(499번째 글자)보다 훨씬 앞(250번째 이전)에만 있으면 단어 경계를
        // 억지로 맞추려다 본문을 과도하게 잘라내는 역효과가 나므로 무시하고 글자수 기준으로 자른다.
        String contentWithEarlySpace = "x " + "y".repeat(600);

        mockMvc.perform(post("/admin/notices/new")
                        .param("title", "제목")
                        .param("content", contentWithEarlySpace)
                        .param("sendNotification", "true"))
                .andExpect(flash().attributeExists("successMessage"));

        then(adminPushService).should().sendToAll(anyString(), eq("x " + "y".repeat(497) + "…"));
    }

    // ── GET 화면 렌더링 ─────────────────────────────────────────────────

    @Test
    void 목록_화면은_공지_페이지를_모델에_담아_렌더링한다() throws Exception {
        given(noticeAdminService.getAdminNotices(any()))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/notices"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/notice/list"))
                .andExpect(model().attributeExists("notices"));
    }

    @Test
    void 등록_폼_화면은_빈_DTO를_모델에_담는다() throws Exception {
        mockMvc.perform(get("/admin/notices/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/notice/create"))
                .andExpect(model().attributeExists("notice"));
    }

    @Test
    void 수정_폼_화면은_조회한_공지를_모델에_담는다() throws Exception {
        given(noticeAdminService.getNoticeForEdit(7L)).willReturn(mock(NoticeResponseDto.class));

        mockMvc.perform(get("/admin/notices/7/edit").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/notice/edit"))
                .andExpect(model().attribute("noticeId", 7L))
                .andExpect(model().attribute("page", 2));
    }

    @Test
    void 수정_폼_화면_대상_공지가_없으면_목록으로_리다이렉트하고_에러메시지를_남긴다() throws Exception {
        willThrow(new NoSuchElementException("공지사항을 찾을 수 없습니다."))
                .given(noticeAdminService).getNoticeForEdit(99L);

        mockMvc.perform(get("/admin/notices/99/edit"))
                .andExpect(redirectedUrl("/admin/notices"))
                .andExpect(flash().attribute("errorMessage", "공지사항을 찾을 수 없습니다."));
    }

    // ── POST 검증 실패 시 폼 재표시 ─────────────────────────────────────

    @Test
    void 등록_제목이_비면_폼을_다시_보여주고_서비스를_호출하지_않는다() throws Exception {
        mockMvc.perform(post("/admin/notices/new")
                        .param("title", "")
                        .param("content", "내용"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/notice/create"))
                .andExpect(model().attributeExists("errors"));

        then(noticeAdminService).should(never()).createNotice(any());
    }

    @Test
    void 수정_제목이_비면_폼을_다시_보여주고_서비스를_호출하지_않는다() throws Exception {
        mockMvc.perform(post("/admin/notices/5/edit")
                        .param("title", "")
                        .param("content", "내용")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/notice/edit"))
                .andExpect(model().attribute("noticeId", 5L));

        then(noticeAdminService).should(never()).updateNotice(any(), any());
    }

    // ── POST 정상 처리 ─────────────────────────────────────────────────

    @Test
    void 수정_성공시_감사로그를_남기고_해당_페이지_목록으로_리다이렉트한다() throws Exception {
        mockMvc.perform(post("/admin/notices/5/edit")
                        .param("title", "제목")
                        .param("content", "내용")
                        .param("page", "3"))
                .andExpect(redirectedUrl("/admin/notices?page=3"))
                .andExpect(flash().attribute("successMessage", "공지사항이 수정되었습니다."));

        then(noticeAdminService).should().updateNotice(eq(5L), any());
        then(adminLogService).should().log(AdminAction.NOTICE_UPDATE, "NOTICE", 5L, "제목");
    }

    @Test
    void 고정_토글_성공시_감사로그를_남기고_목록으로_리다이렉트한다() throws Exception {
        mockMvc.perform(post("/admin/notices/5/pin").param("page", "1"))
                .andExpect(redirectedUrl("/admin/notices?page=1"))
                .andExpect(flash().attribute("successMessage", "고정 상태가 변경되었습니다."));

        then(noticeAdminService).should().togglePin(5L);
        then(adminLogService).should().log(AdminAction.NOTICE_PIN_TOGGLE, "NOTICE", 5L, null);
    }

    @Test
    void 삭제_성공시_감사로그를_남기고_목록으로_리다이렉트한다() throws Exception {
        mockMvc.perform(post("/admin/notices/5/delete"))
                .andExpect(redirectedUrl("/admin/notices?page=0"))
                .andExpect(flash().attribute("successMessage", "공지사항이 삭제되었습니다."));

        then(noticeAdminService).should().deleteNotice(5L);
        then(adminLogService).should().log(AdminAction.NOTICE_DELETE, "NOTICE", 5L, null);
    }

    @Test
    void 삭제_중_예외가_나면_에러메시지를_남기고_목록으로_리다이렉트한다() throws Exception {
        willThrow(new RuntimeException("boom")).given(noticeAdminService).deleteNotice(5L);

        mockMvc.perform(post("/admin/notices/5/delete"))
                .andExpect(redirectedUrl("/admin/notices?page=0"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
