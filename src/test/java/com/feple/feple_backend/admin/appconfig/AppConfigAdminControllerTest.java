package com.feple.feple_backend.admin.appconfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.appconfig.service.AppConfigAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AppConfigAdminControllerTest {

    @Mock
    AppConfigAdminService appConfigAdminService;

    @Mock
    AdminLogService adminLogService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AppConfigAdminController controller = new AppConfigAdminController(
                appConfigAdminService, adminLogService, new ObjectMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void 유효한_입력이면_설정을_저장하고_감사로그를_남긴다() throws Exception {
        mockMvc.perform(post("/admin/app-config")
                        .param("minSupportedVersion", "1.2.0")
                        .param("latestVersion", "1.3.0")
                        .param("maintenance", "true")
                        .param("featureFlags", "{\"chatEnabled\": true}"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/app-config"))
                .andExpect(flash().attribute("successMessage", "앱 설정이 저장되었습니다."));

        then(appConfigAdminService).should().updateConfig(any());
        then(adminLogService).should()
                .log(eq(AdminAction.APP_CONFIG_UPDATE), eq("APP_CONFIG"), isNull(), any());
    }

    @Test
    void featureFlags_JSON이_잘못되면_저장하지_않고_폼을_다시_렌더한다() throws Exception {
        mockMvc.perform(post("/admin/app-config")
                        .param("minSupportedVersion", "1.2.0")
                        .param("latestVersion", "1.3.0")
                        .param("featureFlags", "{chatEnabled: true}"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/system/app-config"))
                .andExpect(model().attributeExists("errors"));

        then(appConfigAdminService).should(never()).updateConfig(any());
    }

    @Test
    void 버전_형식이_잘못되면_저장하지_않고_폼을_다시_렌더한다() throws Exception {
        mockMvc.perform(post("/admin/app-config")
                        .param("minSupportedVersion", "1.2")
                        .param("latestVersion", "1.3.0")
                        .param("featureFlags", "{}"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/system/app-config"))
                .andExpect(model().attributeExists("errors"));

        then(appConfigAdminService).should(never()).updateConfig(any());
    }
}
