package com.feple.feple_backend.admin.user;

import com.feple.feple_backend.admin.user.UserDetailAggregationService;
import com.feple.feple_backend.admin.user.UserDetailDto;
import com.feple.feple_backend.admin.user.UserListCountsDto;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.service.UserAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserAdminControllerTest {

    @Mock UserAdminService userService;
    @Mock UserDetailAggregationService userDetailAggregationService;
    @Mock AdminLogService adminLogService;

    @InjectMocks UserAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private UserListCountsDto emptyCounts() {
        return new UserListCountsDto(Map.of(), Map.of(), Map.of());
    }

    // ── GET /admin/users ──────────────────────────────────────────────────────

    @Test
    void 목록_기본_조회_성공() throws Exception {
        given(userService.getUsersPage(anyInt(), anyInt(), anyString()))
                .willReturn(new PageImpl<>(List.of()));
        given(userDetailAggregationService.getListCounts(any()))
                .willReturn(emptyCounts());

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user/list"))
                .andExpect(model().attributeExists(
                        "users", "keyword", "sort", "filter",
                        "reportCounts", "postCounts", "commentCounts", "extraParams"));
    }

    @Test
    void 목록_filter_banned이면_getBannedUsersPage_호출() throws Exception {
        given(userService.getBannedUsersPage(anyInt(), anyInt(), anyString()))
                .willReturn(new PageImpl<>(List.of()));
        given(userDetailAggregationService.getListCounts(any()))
                .willReturn(emptyCounts());

        mockMvc.perform(get("/admin/users").param("filter", "banned"))
                .andExpect(status().isOk());

        then(userService).should().getBannedUsersPage(anyInt(), anyInt(), anyString());
        then(userService).should(never()).getUsersPage(anyInt(), anyInt(), anyString());
    }

    @Test
    void 목록_sort_reports이면_getUsersPageSortedByReports_호출() throws Exception {
        given(userService.getUsersPageSortedByReports(anyInt(), anyInt(), anyString()))
                .willReturn(new PageImpl<>(List.of()));
        given(userDetailAggregationService.getListCounts(any()))
                .willReturn(emptyCounts());

        mockMvc.perform(get("/admin/users").param("sort", "reports"))
                .andExpect(status().isOk());

        then(userService).should().getUsersPageSortedByReports(anyInt(), anyInt(), anyString());
        then(userService).should(never()).getUsersPage(anyInt(), anyInt(), anyString());
    }

    // ── GET /admin/users/{id} ─────────────────────────────────────────────────

    @Test
    void 상세_조회_성공() throws Exception {
        UserDetailDto detail = new UserDetailDto(
                mock(UserResponseDto.class), null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        given(userDetailAggregationService.getDetail(1L)).willReturn(detail);

        mockMvc.perform(get("/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user/detail"))
                .andExpect(model().attributeExists(
                        "user", "recentPosts", "recentComments", "likedFestivals", "followedArtists"));
    }

    @Test
    void 상세_조회_NoSuchElementException_목록으로_리다이렉트() throws Exception {
        given(userDetailAggregationService.getDetail(99L))
                .willThrow(new NoSuchElementException("존재하지 않는 회원"));

        mockMvc.perform(get("/admin/users/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("errorMessage", "존재하지 않는 회원"));
    }

    @Test
    void 상세_조회_일반_예외_일반_에러메시지() throws Exception {
        given(userDetailAggregationService.getDetail(1L))
                .willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(get("/admin/users/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attribute("errorMessage", "회원 정보를 불러오는 중 오류가 발생했습니다."));
    }

    // ── POST /admin/users/bulk-delete ─────────────────────────────────────────

    @Test
    void 일괄_삭제_ids_없으면_서비스_미호출() throws Exception {
        mockMvc.perform(post("/admin/users/bulk-delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        then(userService).should(never()).bulkDeleteUsers(any());
    }

    @Test
    void 일괄_삭제_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/users/bulk-delete")
                        .param("ids", "1", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "2명 회원이 삭제되었습니다."));

        then(userService).should().bulkDeleteUsers(List.of(1L, 2L));
    }

    // ── POST /admin/users/{id}/delete ─────────────────────────────────────────

    @Test
    void 회원_삭제_성공() throws Exception {
        given(userService.adminDeleteUser(1L)).willReturn("테스트유저");

        mockMvc.perform(post("/admin/users/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "회원이 삭제되었습니다."));

        then(userService).should().adminDeleteUser(1L);
        then(userService).should(never()).getAdminUser(anyLong());
    }

    @Test
    void 회원_삭제_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(userService).adminDeleteUser(1L);

        mockMvc.perform(post("/admin/users/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "회원 삭제에 실패했습니다."));
    }

    // ── POST /admin/users/{id}/role ───────────────────────────────────────────

    @Test
    void 역할_변경_성공() throws Exception {
        mockMvc.perform(post("/admin/users/1/role")
                        .param("role", "ARTIST"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/1"))
                .andExpect(flash().attribute("successMessage", "역할이 변경되었습니다: 아티스트"));

        then(userService).should().updateUserRole(1L, UserRole.ARTIST);
    }

    @Test
    void 역할_변경_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(userService).updateUserRole(1L, UserRole.USER);

        mockMvc.perform(post("/admin/users/1/role")
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/1"))
                .andExpect(flash().attribute("errorMessage", "역할 변경에 실패했습니다."));
    }

    // ── POST /admin/users/{id}/ban ────────────────────────────────────────────

    @Test
    void 회원_7일_정지_성공() throws Exception {
        mockMvc.perform(post("/admin/users/1/ban")
                        .param("days", "7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/1"))
                .andExpect(flash().attribute("successMessage", "7일 정지가 적용되었습니다."));

        then(userService).should().banUser(1L, 7, null);
    }

    @Test
    void 회원_영구_정지_성공() throws Exception {
        mockMvc.perform(post("/admin/users/1/ban")
                        .param("days", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/1"))
                .andExpect(flash().attribute("successMessage", "영구 정지가 적용되었습니다."));

        then(userService).should().banUser(1L, 0, null);
    }

    @Test
    void 회원_정지_사유_포함() throws Exception {
        mockMvc.perform(post("/admin/users/1/ban")
                        .param("days", "3")
                        .param("reason", "욕설"))
                .andExpect(flash().attribute("successMessage", "3일 정지가 적용되었습니다."));

        then(userService).should().banUser(1L, 3, "욕설");
    }

    // ── POST /admin/users/{id}/unban ──────────────────────────────────────────

    @Test
    void 회원_정지_해제_성공() throws Exception {
        mockMvc.perform(post("/admin/users/1/unban"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/1"))
                .andExpect(flash().attribute("successMessage", "정지가 해제되었습니다."));

        then(userService).should().unbanUser(1L);
    }

    @Test
    void 회원_정지_해제_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(userService).unbanUser(1L);

        mockMvc.perform(post("/admin/users/1/unban"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/1"))
                .andExpect(flash().attribute("errorMessage", "정지 해제 중 오류가 발생했습니다."));
    }
}
