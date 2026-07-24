package com.feple.feple_backend.admin.dashboard;

import com.feple.feple_backend.admin.dashboard.ContentTrendDto;
import com.feple.feple_backend.admin.user.UserActivityStatsDto;
import com.feple.feple_backend.admin.service.AdminStatsMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StatsAdminControllerTest {

    @Mock AdminStatsMetrics adminStatsMetrics;
    @InjectMocks StatsAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        given(adminStatsMetrics.getUserActivityStats()).willReturn(new UserActivityStatsDto(0,0,0,0,0,0));
        given(adminStatsMetrics.getRangeStats(any(), any())).willReturn(List.of());
        given(adminStatsMetrics.getContentTrend()).willReturn(mock(ContentTrendDto.class));
    }

    @Test
    void 통계_조회_뷰와_모델_속성_확인() throws Exception {
        mockMvc.perform(get("/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard/stats"))
                .andExpect(model().attributeExists("activityStats", "rangeStats", "contentTrend", "from", "to"));
    }

    @Test
    void to_미지정시_오늘_날짜로_설정() throws Exception {
        mockMvc.perform(get("/admin/stats"))
                .andExpect(model().attribute("to", LocalDate.now().toString()));
    }

    @Test
    void to가_미래_날짜이면_오늘로_클램핑() throws Exception {
        mockMvc.perform(get("/admin/stats").param("to", "9999-12-31"))
                .andExpect(model().attribute("to", LocalDate.now().toString()));
    }

    @Test
    void from_to_모두_지정하면_해당_범위_사용() throws Exception {
        String from = LocalDate.now().minusDays(30).toString();
        String to   = LocalDate.now().toString();

        mockMvc.perform(get("/admin/stats").param("from", from).param("to", to))
                .andExpect(model().attribute("from", from))
                .andExpect(model().attribute("to",   to));
    }
}
