package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.admin.system.AdminPushService;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.service.UserAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminPushControllerTest {

    @Mock AdminPushService adminPushService;
    @Mock AdminLogService adminLogService;
    @Mock UserAdminService userAdminService;

    @InjectMocks AdminPushController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/push ───────────────────────────────────────────────────────

    @Test
    void 푸시_폼_조회_뷰와_모델_속성_확인() throws Exception {
        given(adminPushService.getFormData())
                .willReturn(new PushFormData(0L, List.of(), List.of(), List.of()));

        mockMvc.perform(get("/admin/push"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/system/push"))
                .andExpect(model().attributeExists("deviceCount", "history", "artists", "festivals"));
    }

    // ── POST /admin/push ──────────────────────────────────────────────────────

    @Test
    void 전체_푸시_제목_비어있으면_errorMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/push")
                        .param("title", "")
                        .param("body", "내용"))
                .andExpect(redirectedUrl("/admin/push"))
                .andExpect(flash().attribute("errorMessage", "제목과 내용을 모두 입력해주세요."));
    }

    @Test
    void 전체_푸시_제목_100자_초과_errorMessage_설정() throws Exception {
        String longTitle = "a".repeat(101);

        mockMvc.perform(post("/admin/push")
                        .param("title", longTitle)
                        .param("body", "내용"))
                .andExpect(flash().attribute("errorMessage", "푸시 제목은 100자 이하여야 합니다."));
    }

    @Test
    void 전체_푸시_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/push")
                        .param("title", "공지")
                        .param("body", "테스트 공지입니다."))
                .andExpect(redirectedUrl("/admin/push"))
                .andExpect(flash().attribute("successMessage", "푸시 알림이 발송되었습니다."));
    }

    @Test
    void 전체_푸시_서비스_예외_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("FCM 오류")).given(adminPushService).sendToAll(anyString(), anyString());

        mockMvc.perform(post("/admin/push")
                        .param("title", "공지")
                        .param("body", "내용"))
                .andExpect(flash().attribute("errorMessage", "발송 중 오류가 발생했습니다."));
    }

    // ── GET /admin/push/search-user ───────────────────────────────────────────

    @Test
    void 사용자_검색_성공_JSON_반환() throws Exception {
        UserResponseDto user = mock(UserResponseDto.class);
        given(user.getId()).willReturn(1L);
        given(user.getNickname()).willReturn("tester");
        given(userAdminService.findByNickname("tester")).willReturn(user);

        mockMvc.perform(get("/admin/push/search-user").param("nickname", "tester"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nickname").value("tester"));
    }

    @Test
    void 사용자_검색_없으면_404_반환() throws Exception {
        given(userAdminService.findByNickname("unknown"))
                .willThrow(new NoSuchElementException("없음"));

        mockMvc.perform(get("/admin/push/search-user").param("nickname", "unknown"))
                .andExpect(status().isNotFound());
    }

    // ── POST /admin/push/artist-followers ─────────────────────────────────────

    @Test
    void 아티스트_팔로워_푸시_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/push/artist-followers")
                        .param("title", "공지")
                        .param("body", "팔로워 공지")
                        .param("artistId", "1"))
                .andExpect(redirectedUrl("/admin/push"))
                .andExpect(flash().attribute("successMessage", "아티스트 팔로워에게 발송되었습니다."));
    }

    @Test
    void 아티스트_팔로워_푸시_입력검증_실패_errorMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/push/artist-followers")
                        .param("title", "")
                        .param("body", "내용")
                        .param("artistId", "1"))
                .andExpect(flash().attribute("errorMessage", "제목과 내용을 모두 입력해주세요."));
    }

    // ── POST /admin/push/festival-certified ───────────────────────────────────

    @Test
    void 페스티벌_인증자_푸시_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/push/festival-certified")
                        .param("title", "공지")
                        .param("body", "인증자 공지")
                        .param("festivalId", "1"))
                .andExpect(redirectedUrl("/admin/push"))
                .andExpect(flash().attribute("successMessage", "페스티벌 인증 참여자에게 발송되었습니다."));
    }

    // ── POST /admin/push/test ─────────────────────────────────────────────────

    @Test
    void 테스트_푸시_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/push/test")
                        .param("title", "테스트")
                        .param("body", "내용")
                        .param("targetUserId", "42"))
                .andExpect(redirectedUrl("/admin/push"))
                .andExpect(flash().attributeExists("successMessage"));

        then(adminPushService).should().sendTest(42L, "테스트", "내용");
    }
}
