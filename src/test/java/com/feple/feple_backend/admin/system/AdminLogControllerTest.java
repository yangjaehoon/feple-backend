package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.admin.log.AdminLogService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminLogControllerTest {

    @Mock AdminLogService adminLogService;
    @InjectMocks AdminLogController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void 로그_목록_조회_뷰와_모델_속성_확인() throws Exception {
        given(adminLogService.getLogs(anyInt(), any())).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/logs"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/system/logs"))
                .andExpect(model().attributeExists("logs", "targetType", "adminUsername", "actionLabels"));
    }

    @Test
    void 필터_없으면_extraParams_null() throws Exception {
        given(adminLogService.getLogs(anyInt(), any())).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/logs"))
                .andExpect(model().attribute("extraParams", (Object) null));
    }

    @Test
    void targetType_필터_있으면_extraParams_설정() throws Exception {
        given(adminLogService.getLogs(anyInt(), any())).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/logs").param("targetType", "USER"))
                .andExpect(model().attribute("extraParams", "targetType=USER"));
    }

    @Test
    void adminUsername_필터_있으면_extraParams_설정() throws Exception {
        given(adminLogService.getLogs(anyInt(), any())).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/logs").param("adminUsername", "admin01"))
                .andExpect(model().attribute("extraParams", "adminUsername=admin01"));
    }
}
