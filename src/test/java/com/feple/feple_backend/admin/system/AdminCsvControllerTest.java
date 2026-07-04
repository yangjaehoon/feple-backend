package com.feple.feple_backend.admin.system;

import com.feple.feple_backend.admin.moderation.ReportCsvExporter;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.service.UserAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminCsvControllerTest {

    @Mock UserAdminService userAdminService;
    @Mock AdminLogService adminLogService;

    ReportCsvExporter postExporter = mock(ReportCsvExporter.class);

    AdminCsvController controller;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        given(postExporter.getReportType()).willReturn("post");
        controller = new AdminCsvController(userAdminService, adminLogService, List.of(postExporter));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/export/users.csv ───────────────────────────────────────────

    @Test
    void users_csv_내보내기_성공() throws Exception {
        UserResponseDto user = mock(UserResponseDto.class);
        given(user.getId()).willReturn(1L);
        given(user.getNickname()).willReturn("tester");
        given(user.getEmail()).willReturn("tester@example.com");
        given(user.getRoleDisplayName()).willReturn("일반");
        given(user.isBanned()).willReturn(false);
        given(userAdminService.getAllUsersForExport()).willReturn(List.of(user));

        mockMvc.perform(get("/admin/export/users.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")));
    }

    @Test
    void users_csv_목록_비어있어도_헤더_행_포함() throws Exception {
        given(userAdminService.getAllUsersForExport()).willReturn(List.of());

        mockMvc.perform(get("/admin/export/users.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"));
    }

    // ── GET /admin/export/reports.csv ─────────────────────────────────────────

    @Test
    void reports_csv_post_타입_내보내기_성공() throws Exception {
        given(postExporter.buildCsv()).willReturn("ID,내용\n1,테스트\n");

        mockMvc.perform(get("/admin/export/reports.csv").param("type", "post"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"));
    }

    @Test
    void reports_csv_알_수_없는_타입이면_post_엑스포터_폴백() throws Exception {
        given(postExporter.buildCsv()).willReturn("ID,내용\n");

        mockMvc.perform(get("/admin/export/reports.csv").param("type", "unknown"))
                .andExpect(status().isOk());

        then(postExporter).should().buildCsv();
    }

    @Test
    void reports_csv_엑스포터_없으면_400_반환() throws Exception {
        AdminCsvController emptyController =
                new AdminCsvController(userAdminService, adminLogService, List.of());
        MockMvc emptyMvc = MockMvcBuilders.standaloneSetup(emptyController).build();

        emptyMvc.perform(get("/admin/export/reports.csv").param("type", "unknown"))
                .andExpect(status().isBadRequest());
    }
}
