package com.feple.feple_backend.userreport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.feple.feple_backend.global.exception.GlobalExceptionHandler;
import com.feple.feple_backend.support.AuthTestHelper;
import com.feple.feple_backend.userreport.service.UserReportService;
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

@ExtendWith(MockitoExtension.class)
class UserReportControllerTest {

    @Mock UserReportService userReportService;

    @InjectMocks UserReportController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 사용자_신고_성공시_201과_서비스_위임() throws Exception {
        mockMvc.perform(post("/users/5/report")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"SPAM\",\"detail\":\"광고 도배\"}"))
                .andExpect(status().isCreated());

        then(userReportService).should().submitReport(eq(5L), eq(1L), any());
    }

    @Test
    void 사용자_신고_사유_없으면_400() throws Exception {
        mockMvc.perform(post("/users/5/report")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        then(userReportService).shouldHaveNoInteractions();
    }
}
