package com.feple.feple_backend.artist.suggestion.controller;

import com.feple.feple_backend.artist.suggestion.dto.ArtistSuggestionResponseDto;
import com.feple.feple_backend.artist.suggestion.service.ArtistSuggestionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArtistSuggestionControllerTest {

    @Mock ArtistSuggestionService artistSuggestionService;

    @InjectMocks ArtistSuggestionController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void 아티스트_제안_성공() throws Exception {
        ArtistSuggestionResponseDto dto = mock(ArtistSuggestionResponseDto.class);
        given(artistSuggestionService.submit(eq(1L), any())).willReturn(dto);

        mockMvc.perform(post("/artist-suggestions")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"artistName\":\"테스트 아티스트\",\"note\":\"제안 메모\"}"))
                .andExpect(status().isCreated());
    }
}
