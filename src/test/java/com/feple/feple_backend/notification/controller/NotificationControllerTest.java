package com.feple.feple_backend.notification.controller;

import com.feple.feple_backend.notification.service.NotificationQueryService;
import com.feple.feple_backend.support.AuthTestHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.SpringDataJacksonConfiguration;
import org.springframework.data.web.config.SpringDataWebSettings;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock NotificationQueryService notificationQueryService;

    @InjectMocks NotificationController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // PageImpl 직렬화를 위해 ObjectMapper에 SpringDataJacksonConfiguration.PageModule 등록
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        SpringDataWebSettings settings = new SpringDataWebSettings(
                EnableSpringDataWebSupport.PageSerializationMode.DIRECT);
        objectMapper.registerModule(new SpringDataJacksonConfiguration.PageModule(settings));
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver(),
                        new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void 알림_목록_조회() throws Exception {
        given(notificationQueryService.getMyNotifications(eq(1L), any()))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/notifications")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 읽지않은_알림_수_조회() throws Exception {
        given(notificationQueryService.getUnreadCount(1L)).willReturn(3L);

        mockMvc.perform(get("/notifications/unread-count")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void 알림_읽음_처리() throws Exception {
        mockMvc.perform(patch("/notifications/5/read")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    void 전체_읽음_처리() throws Exception {
        mockMvc.perform(patch("/notifications/read-all")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isNoContent());
    }
}
