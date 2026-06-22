package com.feple.feple_backend.notification.controller;

import com.feple.feple_backend.notification.dto.NotificationPreferenceDto;
import com.feple.feple_backend.notification.service.NotificationPreferenceService;
import com.feple.feple_backend.support.AuthTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceControllerTest {

    @Mock NotificationPreferenceService preferenceService;

    @InjectMocks NotificationPreferenceController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void 알림_설정_조회() throws Exception {
        NotificationPreferenceDto dto = mock(NotificationPreferenceDto.class);
        given(preferenceService.getPreferences(1L)).willReturn(dto);

        mockMvc.perform(get("/users/me/notification-preferences")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 알림_설정_수정() throws Exception {
        NotificationPreferenceDto dto = mock(NotificationPreferenceDto.class);
        given(preferenceService.updatePreferences(eq(1L), any())).willReturn(dto);

        mockMvc.perform(put("/users/me/notification-preferences")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"certEnabled\":true,\"commentEnabled\":true,\"festivalEnabled\":false,\"songRequestEnabled\":false}"))
                .andExpect(status().isOk());
    }
}
