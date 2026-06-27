package com.feple.feple_backend.artist.controller;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.artistfestival.dto.ArtistScheduleResponseDto;
import com.feple.feple_backend.artistfestival.service.ArtistScheduleService;
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
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArtistControllerTest {

    @Mock ArtistService artistService;
    @Mock ArtistScheduleService artistScheduleService;

    @InjectMocks ArtistController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void 아티스트_목록_조회() throws Exception {
        ArtistResponseDto dto = mock(ArtistResponseDto.class);
        given(dto.getId()).willReturn(1L);
        given(artistService.getAllArtists()).willReturn(List.of(dto));

        mockMvc.perform(get("/artists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void 아티스트_단건_조회() throws Exception {
        ArtistResponseDto dto = mock(ArtistResponseDto.class);
        given(artistService.getArtistById(1L)).willReturn(dto);

        mockMvc.perform(get("/artists/1"))
                .andExpect(status().isOk());
    }

    @Test
    void 아티스트_스케줄_조회() throws Exception {
        ArtistScheduleResponseDto scheduleResponse = mock(ArtistScheduleResponseDto.class);
        given(artistScheduleService.getArtistSchedule(1L)).willReturn(List.of(scheduleResponse));

        mockMvc.perform(get("/artists/1/schedule"))
                .andExpect(status().isOk());
    }
}
