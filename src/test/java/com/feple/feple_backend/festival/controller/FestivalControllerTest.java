package com.feple.feple_backend.festival.controller;

import com.feple.feple_backend.artist.song.service.SongAdminService;
import com.feple.feple_backend.artist.song.service.SongService;
import com.feple.feple_backend.festival.dto.FestivalDetailResponseDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.dto.WeatherDto;
import com.feple.feple_backend.festival.service.FestivalAttendanceService;
import com.feple.feple_backend.festival.service.FestivalLikeService;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.festival.service.WeatherService;
import com.feple.feple_backend.support.AuthTestHelper;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FestivalControllerTest {

    @Mock FestivalService festivalService;
    @Mock FestivalLikeService festivalLikeService;
    @Mock FestivalAttendanceService festivalAttendanceService;
    @Mock WeatherService weatherService;
    @Mock SongService songService;
    @Mock SongAdminService songAdminService;

    @InjectMocks FestivalController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void 페스티벌_목록_조회() throws Exception {
        given(festivalService.getAllFestivals(any())).willReturn(List.of());

        mockMvc.perform(get("/festivals"))
                .andExpect(status().isOk());
    }

    @Test
    void 페스티벌_단건_조회() throws Exception {
        FestivalDetailResponseDto dto = mock(FestivalDetailResponseDto.class);
        given(festivalService.getFestivalDetail(1L)).willReturn(dto);

        mockMvc.perform(get("/festivals/1"))
                .andExpect(status().isOk());
    }

    @Test
    void 좋아요_토글() throws Exception {
        given(festivalLikeService.toggleLike(1L, 1L)).willReturn(true);

        mockMvc.perform(post("/festivals/1/like")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 좋아요_상태_미인증이면_false_반환() throws Exception {
        mockMvc.perform(get("/festivals/1/liked"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void 참석_토글() throws Exception {
        given(festivalAttendanceService.toggleAttending(1L, 1L)).willReturn(true);

        mockMvc.perform(post("/festivals/1/attending")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 날씨_조회_없으면_204() throws Exception {
        given(weatherService.getByFestivalId(1L)).willReturn(Optional.empty());

        mockMvc.perform(get("/festivals/1/weather"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 날씨_조회_있으면_200() throws Exception {
        WeatherDto weatherDto = mock(WeatherDto.class);
        given(weatherService.getByFestivalId(1L)).willReturn(Optional.of(weatherDto));

        mockMvc.perform(get("/festivals/1/weather"))
                .andExpect(status().isOk());
    }

    @Test
    void 세트리스트_조회() throws Exception {
        given(songService.getFestivalSetlist(1L)).willReturn(List.of());

        mockMvc.perform(get("/festivals/1/setlist"))
                .andExpect(status().isOk());
    }
}
