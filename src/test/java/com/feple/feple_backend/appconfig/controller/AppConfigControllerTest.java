package com.feple.feple_backend.appconfig.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.feple.feple_backend.appconfig.dto.AppConfigResponseDto;
import com.feple.feple_backend.appconfig.service.AppConfigService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AppConfigControllerTest {

    @Mock
    AppConfigService appConfigService;

    @InjectMocks
    AppConfigController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void 앱_설정을_조회한다() throws Exception {
        given(appConfigService.getAppConfig()).willReturn(new AppConfigResponseDto(
                "1.2.0", "1.3.0", false, null, "점검 예정", Map.of("chatEnabled", true)));

        mockMvc.perform(get("/app/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minSupportedVersion").value("1.2.0"))
                .andExpect(jsonPath("$.latestVersion").value("1.3.0"))
                .andExpect(jsonPath("$.maintenance").value(false))
                .andExpect(jsonPath("$.noticeMessage").value("점검 예정"))
                .andExpect(jsonPath("$.features.chatEnabled").value(true));
    }
}
