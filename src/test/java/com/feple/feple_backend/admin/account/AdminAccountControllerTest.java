package com.feple.feple_backend.admin.account;

import com.feple.feple_backend.admin.log.AdminLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminAccountControllerTest {

    @Mock AdminAccountService accountService;
    @Mock AdminLogService adminLogService;

    @InjectMocks AdminAccountController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/accounts ───────────────────────────────────────────────────

    @Test
    void 목록_조회_뷰와_모델_속성_확인() throws Exception {
        given(accountService.findAll()).willReturn(List.of());

        mockMvc.perform(get("/admin/accounts"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/account/list"))
                .andExpect(model().attributeExists("accounts", "allPermissions", "allRoles"));
    }

    // ── GET /admin/accounts/new ───────────────────────────────────────────────

    @Test
    void 신규_폼_조회() throws Exception {
        mockMvc.perform(get("/admin/accounts/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/account/form"))
                .andExpect(model().attributeExists("allPermissions", "allRoles"));
    }

    // ── POST /admin/accounts ──────────────────────────────────────────────────

    @Test
    void 계정_생성_성공_successMessage_설정() throws Exception {
        AdminAccount created = mock(AdminAccount.class);
        given(created.getId()).willReturn(1L);
        given(created.getUsername()).willReturn("newadmin");
        given(accountService.create(any())).willReturn(created);

        MockMultipartFile emptyFile = new MockMultipartFile("profileImage", new byte[0]);

        mockMvc.perform(multipart("/admin/accounts")
                        .file(emptyFile)
                        .param("username", "newadmin")
                        .param("password", "secret123")
                        .param("displayName", "새관리자")
                        .param("role", "MANAGER")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attribute("successMessage", "관리자 계정이 생성되었습니다."));

        then(accountService).should().create(any(AdminAccountCreateRequestDto.class));
    }

    @Test
    void 계정_생성_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("중복 계정")).given(accountService).create(any());

        MockMultipartFile emptyFile = new MockMultipartFile("profileImage", new byte[0]);

        mockMvc.perform(multipart("/admin/accounts")
                        .file(emptyFile)
                        .param("username", "newadmin")
                        .param("password", "secret")
                        .param("role", "MANAGER")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attribute("errorMessage", "계정 생성 중 오류가 발생했습니다."));
    }

    // ── GET /admin/accounts/{id}/edit ─────────────────────────────────────────

    @Test
    void 편집_폼_조회() throws Exception {
        given(accountService.findById(1L)).willReturn(mock(AdminAccount.class));

        mockMvc.perform(get("/admin/accounts/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/account/form"))
                .andExpect(model().attributeExists("account", "allPermissions", "allRoles"));
    }

    // ── POST /admin/accounts/{id}/update ─────────────────────────────────────

    @Test
    void 계정_수정_성공_successMessage_설정() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile("profileImage", new byte[0]);

        mockMvc.perform(multipart("/admin/accounts/1/update")
                        .file(emptyFile)
                        .param("role", "MANAGER")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attribute("successMessage", "관리자 계정이 수정되었습니다."));

        then(accountService).should().update(eq(1L), any(AdminAccountUpdateRequestDto.class));
    }

    @Test
    void 계정_수정_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(accountService).update(anyLong(), any());

        mockMvc.perform(multipart("/admin/accounts/1/update")
                        .file(new MockMultipartFile("profileImage", new byte[0]))
                        .param("role", "MANAGER")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(flash().attribute("errorMessage", "계정 수정 중 오류가 발생했습니다."));
    }

    // ── POST /admin/accounts/{id}/delete ─────────────────────────────────────

    @Test
    void 계정_삭제_성공_successMessage_설정() throws Exception {
        AdminAccount deleted = mock(AdminAccount.class);
        given(deleted.getUsername()).willReturn("target");
        given(accountService.delete(1L, "admin")).willReturn(deleted);

        mockMvc.perform(post("/admin/accounts/1/delete")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attribute("successMessage", "관리자 계정이 삭제되었습니다."));

        then(accountService).should().delete(1L, "admin");
    }

    @Test
    void 계정_삭제_실패_errorMessage_설정() throws Exception {
        willThrow(new IllegalArgumentException("자신의 계정 삭제 불가"))
                .given(accountService).delete(anyLong(), anyString());

        mockMvc.perform(post("/admin/accounts/1/delete")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(flash().attribute("errorMessage", "자신의 계정 삭제 불가"));
    }

    // ── POST /admin/accounts/{id}/toggle-enabled ──────────────────────────────

    @Test
    void 활성화_토글_성공_successMessage_설정() throws Exception {
        AdminAccount toggled = mock(AdminAccount.class);
        given(toggled.getUsername()).willReturn("target");
        given(toggled.isEnabled()).willReturn(false);
        given(accountService.toggleEnabled(1L, "admin")).willReturn(toggled);

        mockMvc.perform(post("/admin/accounts/1/toggle-enabled")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(redirectedUrl("/admin/accounts"))
                .andExpect(flash().attribute("successMessage", "계정 활성화 상태가 변경되었습니다."));

        then(accountService).should().toggleEnabled(1L, "admin");
    }

    @Test
    void 활성화_토글_실패_errorMessage_설정() throws Exception {
        willThrow(new IllegalArgumentException("마지막 SUPER_ADMIN 비활성화 불가"))
                .given(accountService).toggleEnabled(anyLong(), anyString());

        mockMvc.perform(post("/admin/accounts/1/toggle-enabled")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(flash().attribute("errorMessage", "마지막 SUPER_ADMIN 비활성화 불가"));
    }
}
