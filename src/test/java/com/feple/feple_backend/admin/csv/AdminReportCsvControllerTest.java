package com.feple.feple_backend.admin.csv;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.feple.feple_backend.admin.log.AdminLogService;
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
    ReportCsvExporter photoExporter = mock(ReportCsvExporter.class);

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        given(postExporter.getReportType()).willReturn("post");
        given(photoExporter.getReportType()).willReturn("photo");
        AdminReportCsvController controller =
                new AdminReportCsvController(adminLogService, List.of(postExporter, photoExporter));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void reports_csv_photo_타입은_photo_엑스포터로_라우팅되고_post로_폴백하지_않음() throws Exception {
        given(photoExporter.buildCsv()).willReturn("ID,내용\n1,사진신고\n");

        mockMvc.perform(get("/admin/export/reports.csv").param("type", "photo"))
                .andExpect(status().isOk());

        then(photoExporter).should().buildCsv();
        then(postExporter).should(never()).buildCsv();
    }

    @Test
    void reports_csv_post_타입_내보내기_성공() throws Exception {
        given(postExporter.buildCsv()).willReturn("ID,내용\n1,테스트\n");

        mockMvc.perform(get("/admin/export/reports.csv").param("type", "post"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"));
    }

    @Test
    void reports_csv_알_수_없는_타입이면_400_반환하고_post로_폴백하지_않음() throws Exception {
        mockMvc.perform(get("/admin/export/reports.csv").param("type", "unknown"))
                .andExpect(status().isBadRequest());

        then(postExporter).should(never()).buildCsv();
    }

    @Test
    void reports_csv_엑스포터_없으면_400_반환() throws Exception {
        AdminReportCsvController emptyController = new AdminReportCsvController(adminLogService, List.of());
        MockMvc emptyMvc = MockMvcBuilders.standaloneSetup(emptyController).build();

        emptyMvc.perform(get("/admin/export/reports.csv").param("type", "unknown"))
                .andExpect(status().isBadRequest());
    }
}
