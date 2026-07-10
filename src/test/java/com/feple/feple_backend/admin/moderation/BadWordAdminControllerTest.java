package com.feple.feple_backend.admin.moderation;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.artist.service.ArtistAdminService;
import com.feple.feple_backend.badword.service.BadWordService;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.nickname.service.NicknameRestrictionService;
import com.feple.feple_backend.post.service.PostAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BadWordAdminControllerTest {

    @Mock BadWordService badWordService;
    @Mock PostAdminService postAdminService;
    @Mock CommentService commentService;
    @Mock AdminLogService adminLogService;
    @Mock NicknameRestrictionService nicknameRestrictionService;
    @Mock ArtistAdminService artistService;

    @InjectMocks BadWordAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/bad-words ──────────────────────────────────────────────────

    @Test
    void 목록_조회_뷰와_모델_속성_확인() throws Exception {
        given(badWordService.findAll()).willReturn(List.of());
        given(nicknameRestrictionService.findAll()).willReturn(List.of());
        given(artistService.getAllArtistsSortedByName()).willReturn(List.of());

        mockMvc.perform(get("/admin/bad-words"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/moderation/bad-words"))
                .andExpect(model().attributeExists("badWords", "nicknameRestrictions", "allArtists"));
    }

    // ── POST /admin/bad-words/add ─────────────────────────────────────────────

    @Test
    void 금칙어_추가_성공() throws Exception {
        mockMvc.perform(post("/admin/bad-words/add").param("word", "욕설"))
                .andExpect(redirectedUrl("/admin/bad-words"))
                .andExpect(flash().attribute("successMessage", "금칙어가 추가되었습니다."));

        then(badWordService).should().add("욕설");
    }

    @Test
    void 금칙어_추가_실패_errorMessage_설정() throws Exception {
        willThrow(new IllegalArgumentException("이미 등록된 단어입니다.")).given(badWordService).add(anyString());

        mockMvc.perform(post("/admin/bad-words/add").param("word", "욕설"))
                .andExpect(flash().attribute("errorMessage", "이미 등록된 단어입니다."));
    }

    // ── GET /admin/bad-words/scan ─────────────────────────────────────────────

    @Test
    void 금칙어_스캔_성공_postCount_commentCount_반환() throws Exception {
        given(postAdminService.countPostsContaining("욕설")).willReturn(3L);
        given(commentService.countCommentsContaining("욕설")).willReturn(7L);

        mockMvc.perform(get("/admin/bad-words/scan").param("word", "욕설"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postCount").value(3))
                .andExpect(jsonPath("$.commentCount").value(7));
    }

    @Test
    void 금칙어_스캔_빈_단어_400_반환() throws Exception {
        mockMvc.perform(get("/admin/bad-words/scan").param("word", "  "))
                .andExpect(status().isBadRequest());
    }

    // ── POST /admin/bad-words/{id}/delete ─────────────────────────────────────

    @Test
    void 금칙어_삭제_성공() throws Exception {
        mockMvc.perform(post("/admin/bad-words/1/delete"))
                .andExpect(redirectedUrl("/admin/bad-words"))
                .andExpect(flash().attribute("successMessage", "삭제되었습니다."));

        then(badWordService).should().delete(1L);
    }

    // ── POST /admin/bad-words/nickname-restrictions/add ───────────────────────

    @Test
    void 닉네임_제한_추가_성공() throws Exception {
        mockMvc.perform(post("/admin/bad-words/nickname-restrictions/add").param("word", "금지어"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "닉네임 제한 단어가 추가되었습니다."));

        then(nicknameRestrictionService).should().add("금지어");
    }

    // ── POST /admin/bad-words/nickname-restrictions/{id}/delete ──────────────

    @Test
    void 닉네임_제한_삭제_성공() throws Exception {
        mockMvc.perform(post("/admin/bad-words/nickname-restrictions/5/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "삭제되었습니다."));

        then(nicknameRestrictionService).should().delete(5L);
    }
}
