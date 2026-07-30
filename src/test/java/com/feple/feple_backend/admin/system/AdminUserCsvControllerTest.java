package com.feple.feple_backend.admin.system;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.moderation.UserCsvExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminUserCsvControllerTest {

    @Mock UserCsvExporter userCsvExporter;
    @Mock AdminLogService adminLogService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminUserCsvController controller = new AdminUserCsvController(userCsvExporter, adminLogService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void users_csv_내보내기_성공() throws Exception {
        given(userCsvExporter.buildCsv()).willReturn("ID,닉네임,이메일,역할,가입일,정지여부\n1,tester,tester@example.com,일반,,\n");

        mockMvc.perform(get("/admin/export/users.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")));
    }

    @Test
    void users_csv_목록_비어있어도_헤더_행_포함() throws Exception {
        given(userCsvExporter.buildCsv()).willReturn("ID,닉네임,이메일,역할,가입일,정지여부\n");

        mockMvc.perform(get("/admin/export/users.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"));
    }
}
