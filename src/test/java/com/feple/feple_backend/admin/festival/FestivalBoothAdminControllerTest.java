package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.booth.service.BoothService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FestivalBoothAdminControllerTest {

    @Mock BoothService boothService;
    @Mock AdminLogService adminLogService;

    @InjectMocks FestivalBoothAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── POST /admin/festivals/{festivalId}/booths ─────────────────────────────

    @Test
    void 부스_생성_검증실패_errorMessage_설정() throws Exception {
        // @NotBlank name 없음
        mockMvc.perform(multipart("/admin/festivals/1/booths")
                        .file("boothImageFile", new byte[0])
                        .param("boothType", "FOOD"))
                .andExpect(redirectedUrl("/admin/festivals/1#booths"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void 부스_생성_위치없으면_errorMessage_설정() throws Exception {
        // name, boothType 있지만 latitude/longitude 없음
        mockMvc.perform(multipart("/admin/festivals/1/booths")
                        .file("boothImageFile", new byte[0])
                        .param("name", "푸드 부스")
                        .param("boothType", "FOOD"))
                .andExpect(redirectedUrl("/admin/festivals/1#booths"))
                .andExpect(flash().attribute("errorMessage", "지도에서 위치를 선택해주세요."));
    }

    @Test
    void 부스_생성_성공_successMessage_설정() throws Exception {
        mockMvc.perform(multipart("/admin/festivals/1/booths")
                        .file("boothImageFile", new byte[0])
                        .param("name", "푸드 부스")
                        .param("boothType", "FOOD")
                        .param("latitude", "37.5")
                        .param("longitude", "127.0"))
                .andExpect(redirectedUrl("/admin/festivals/1#booths"))
                .andExpect(flash().attribute("successMessage", "부스가 추가되었습니다."));
    }

    @Test
    void 부스_생성_서비스_예외_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(boothService).createBooth(anyLong(), any());

        mockMvc.perform(multipart("/admin/festivals/1/booths")
                        .file("boothImageFile", new byte[0])
                        .param("name", "푸드 부스")
                        .param("boothType", "FOOD")
                        .param("latitude", "37.5")
                        .param("longitude", "127.0"))
                .andExpect(flash().attribute("errorMessage", "부스 추가에 실패했습니다."));
    }

    // ── POST /admin/festivals/{festivalId}/booths/{boothId}/delete ────────────

    @Test
    void 부스_삭제_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/festivals/1/booths/5/delete"))
                .andExpect(redirectedUrl("/admin/festivals/1#booths"))
                .andExpect(flash().attribute("successMessage", "부스가 삭제되었습니다."));

        then(boothService).should().deleteBooth(1L, 5L);
    }

    @Test
    void 부스_삭제_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(boothService).deleteBooth(anyLong(), anyLong());

        mockMvc.perform(post("/admin/festivals/1/booths/5/delete"))
                .andExpect(flash().attribute("errorMessage", "부스 삭제에 실패했습니다."));
    }
}
