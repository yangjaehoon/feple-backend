package com.feple.feple_backend.admin.system;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.admin.moderation.ReportCsvExporter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminReportCsvControllerTest {

    @Mock AdminLogService adminLogService;

    ReportCsvExporter postExporter = mock(ReportCsvExporter.class);

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        given(postExporter.getReportType()).willReturn("post");
        AdminReportCsvController controller = new AdminReportCsvController(adminLogService, List.of(postExporter));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

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
        AdminReportCsvController emptyController = new AdminReportCsvController(adminLogService, List.of());
        MockMvc emptyMvc = MockMvcBuilders.standaloneSetup(emptyController).build();

        emptyMvc.perform(get("/admin/export/reports.csv").param("type", "unknown"))
                .andExpect(status().isBadRequest());
    }
}
