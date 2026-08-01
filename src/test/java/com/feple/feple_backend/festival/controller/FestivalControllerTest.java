package com.feple.feple_backend.festival.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.feple.feple_backend.artist.song.service.SongAdminService;
import com.feple.feple_backend.artist.song.service.SongService;
import com.feple.feple_backend.festival.dto.FestivalDetailResponseDto;
import com.feple.feple_backend.festival.dto.WeatherDto;
import com.feple.feple_backend.festival.service.FestivalAttendanceService;
import com.feple.feple_backend.festival.service.FestivalLikeService;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.festival.service.WeatherService;
import com.feple.feple_backend.festival.setlistchangerequest.service.SetlistChangeRequestService;
import com.feple.feple_backend.support.AuthTestHelper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FestivalControllerTest {

    @Mock FestivalService festivalService;
    @Mock FestivalLikeService festivalLikeService;
    @Mock FestivalAttendanceService festivalAttendanceService;
    @Mock WeatherService weatherService;
    @Mock SongService songService;
    @Mock SongAdminService songAdminService;
    @Mock SetlistChangeRequestService setlistChangeRequestService;

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
    void 페스티벌_페이지_조회() throws Exception {
        given(festivalService.getFestivalsPage(any(), any()))
                .willReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        mockMvc.perform(get("/festivals/page").param("page", "0").param("size", "20"))
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

    @Test
    void 좋아요_상태_로그인시_서비스_위임() throws Exception {
        given(festivalLikeService.isLiked(1L, 1L)).willReturn(true);

        mockMvc.perform(get("/festivals/1/liked")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void 참석_상태_미인증이면_false_반환() throws Exception {
        mockMvc.perform(get("/festivals/1/attending"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void 참석_상태_로그인시_서비스_위임() throws Exception {
        given(festivalAttendanceService.isAttending(1L, 1L)).willReturn(true);

        mockMvc.perform(get("/festivals/1/attending")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void 셋리스트_변경_요청_제출_성공() throws Exception {
        mockMvc.perform(post("/festivals/1/setlist-requests")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"artistFestivalId\":100,\"message\":\"곡 순서를 바꿔주세요\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void 셋리스트_변경_요청_메시지_없으면_400() throws Exception {
        mockMvc.perform(post("/festivals/1/setlist-requests")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"artistFestivalId\":100}"))
                .andExpect(status().isBadRequest());
    }
}
