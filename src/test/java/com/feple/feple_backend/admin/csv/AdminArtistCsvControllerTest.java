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
class AdminArtistCsvControllerTest {

    @Mock ArtistCsvExporter artistCsvExporter;
    @Mock AdminLogService adminLogService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminArtistCsvController controller = new AdminArtistCsvController(artistCsvExporter, adminLogService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void artists_csv_내보내기_성공() throws Exception {
        given(artistCsvExporter.buildCsv()).willReturn("ID,이름,영어이름,카테고리,팔로워수,곡수\n1,아이유,IU,발라드,10,3\n");

        mockMvc.perform(get("/admin/export/artists.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")));
    }
}
