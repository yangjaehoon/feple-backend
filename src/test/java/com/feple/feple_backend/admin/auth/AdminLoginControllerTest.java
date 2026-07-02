package com.feple.feple_backend.admin.auth;

import com.feple.feple_backend.admin.AdminLoginFailureHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminLoginControllerTest {

    @InjectMocks AdminLoginController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void 로그인_폼_조회_세션에_에러없음() throws Exception {
        mockMvc.perform(get("/admin/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/auth/login"))
                .andExpect(model().attributeDoesNotExist("loginError"));
    }

    @Test
    void 로그인_폼_조회_세션에_에러있으면_모델에_추가() throws Exception {
        mockMvc.perform(get("/admin/login")
                        .sessionAttr(AdminLoginFailureHandler.SESSION_KEY, "invalid"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("loginError", "invalid"));
    }

    @Test
    void 접근_거부_페이지_조회() throws Exception {
        mockMvc.perform(get("/admin/access-denied"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/auth/access-denied"));
    }
}
