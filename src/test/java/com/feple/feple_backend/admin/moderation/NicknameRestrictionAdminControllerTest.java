package com.feple.feple_backend.admin.moderation;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.nickname.service.NicknameRestrictionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NicknameRestrictionAdminControllerTest {

    @Mock NicknameRestrictionService nicknameRestrictionService;
    @Mock ArtistAdminService artistService;
    @Mock AdminLogService adminLogService;

    @InjectMocks NicknameRestrictionAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/nickname-restrictions ──────────────────────────────────────

    @Test
    void 목록_조회_뷰와_모델_속성_확인() throws Exception {
        given(nicknameRestrictionService.findAll()).willReturn(List.of());
        given(artistService.getAllArtistsSortedByName()).willReturn(List.of());

        mockMvc.perform(get("/admin/nickname-restrictions"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/moderation/nickname-restrictions"))
                .andExpect(model().attributeExists("nicknameRestrictions", "allArtists"));
    }

    // ── POST /admin/nickname-restrictions/add ─────────────────────────────────

    @Test
    void 닉네임_제한_추가_성공() throws Exception {
        mockMvc.perform(post("/admin/nickname-restrictions/add").param("word", "금지어"))
                .andExpect(redirectedUrl("/admin/nickname-restrictions"))
                .andExpect(flash().attribute("successMessage", "닉네임 제한 단어가 추가되었습니다."));

        then(nicknameRestrictionService).should().add("금지어");
    }

    // ── POST /admin/nickname-restrictions/{id}/delete ─────────────────────────

    @Test
    void 닉네임_제한_삭제_성공() throws Exception {
        mockMvc.perform(post("/admin/nickname-restrictions/5/delete"))
                .andExpect(redirectedUrl("/admin/nickname-restrictions"))
                .andExpect(flash().attribute("successMessage", "삭제되었습니다."));

        then(nicknameRestrictionService).should().delete(5L);
    }
}
