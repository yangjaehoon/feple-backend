package com.feple.feple_backend.artistfestival.controller;

import com.feple.feple_backend.artistfestival.service.ArtistFestivalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArtistFestivalControllerTest {

    @Mock ArtistFestivalService artistFestivalService;

    @InjectMocks ArtistFestivalController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void 아티스트_페스티벌_목록_조회_성공() throws Exception {
        given(artistFestivalService.getArtistFestivals(1L)).willReturn(List.of());

        mockMvc.perform(get("/festivals/1/artists"))
                .andExpect(status().isOk());
    }
}
