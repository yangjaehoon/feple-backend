package com.feple.feple_backend.artist.song.controller;

import com.feple.feple_backend.artist.song.dto.SongRequestResponseDto;
import com.feple.feple_backend.artist.song.service.SongRequestService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SongRequestControllerTest {

    @Mock SongRequestService songRequestService;

    @InjectMocks SongRequestController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void 노래_신청_성공() throws Exception {
        SongRequestResponseDto dto = mock(SongRequestResponseDto.class);
        given(songRequestService.submit(eq(1L), eq(1L), any())).willReturn(dto);

        mockMvc.perform(post("/artists/1/song-requests")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"songTitle\":\"테스트 곡\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void 내_노래_신청_목록_조회() throws Exception {
        given(songRequestService.getMyRequests(1L, 1L)).willReturn(List.of());

        mockMvc.perform(get("/artists/1/song-requests")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }
}
