package com.feple.feple_backend.festival.suggestion.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.feple.feple_backend.festival.suggestion.dto.FestivalSuggestionResponseDto;
import com.feple.feple_backend.festival.suggestion.service.FestivalSuggestionService;
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

@ExtendWith(MockitoExtension.class)
class FestivalSuggestionControllerTest {

    @Mock FestivalSuggestionService festivalSuggestionService;

    @InjectMocks FestivalSuggestionController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void 페스티벌_제안_성공() throws Exception {
        FestivalSuggestionResponseDto dto = mock(FestivalSuggestionResponseDto.class);
        given(festivalSuggestionService.submit(eq(1L), any())).willReturn(dto);

        mockMvc.perform(post("/festival-suggestions")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"festivalName\":\"테스트 페스티벌\",\"note\":\"제안 메모\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void 페스티벌명_없으면_400() throws Exception {
        mockMvc.perform(post("/festival-suggestions")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"제안 메모\"}"))
                .andExpect(status().isBadRequest());
    }
}
