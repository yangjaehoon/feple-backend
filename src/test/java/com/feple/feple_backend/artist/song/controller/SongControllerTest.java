package com.feple.feple_backend.artist.song.controller;

import com.feple.feple_backend.artist.song.service.SongService;
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
class SongControllerTest {

    @Mock SongService songService;

    @InjectMocks SongController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void 아티스트_곡_목록_조회() throws Exception {
        given(songService.getSongsByArtistId(1L)).willReturn(List.of());

        mockMvc.perform(get("/artists/1/songs"))
                .andExpect(status().isOk());
    }

    @Test
    void 곡_페스티벌_목록_조회() throws Exception {
        given(songService.getSongFestivals(2L)).willReturn(List.of());

        mockMvc.perform(get("/artists/1/songs/2/festivals"))
                .andExpect(status().isOk());
    }
}
