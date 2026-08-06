package com.feple.feple_backend.admin.csv;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.feple.feple_backend.admin.log.AdminLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AdminFestivalCsvControllerTest {

    @Mock FestivalCsvExporter festivalCsvExporter;
    @Mock AdminLogService adminLogService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminFestivalCsvController controller = new AdminFestivalCsvController(festivalCsvExporter, adminLogService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void festivals_csv_내보내기_성공() throws Exception {
        given(festivalCsvExporter.buildCsv()).willReturn("ID,제목,영어제목,지역,장소,시작일,종료일,좋아요,참석의사\n1,록페,,서울,,,,0,0\n");

        mockMvc.perform(get("/admin/export/festivals.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")));
    }
}
