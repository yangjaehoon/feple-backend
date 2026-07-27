package com.feple.feple_backend.admin.point;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.feple.feple_backend.user.service.PointService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PointAdminControllerTest {

    @Mock PointService pointService;

    @InjectMocks PointAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void 목록_조회_뷰와_모델_속성_확인() throws Exception {
        given(pointService.getAllPointLogs(anyInt(), anyInt(), any())).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/points"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/point/list"))
                .andExpect(model().attributeExists("logs", "keyword"));
    }

    @Test
    void 키워드로_검색시_서비스에_그대로_전달된다() throws Exception {
        given(pointService.getAllPointLogs(eq(2), anyInt(), eq("닉네임"))).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/points").param("page", "2").param("keyword", "닉네임"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("keyword", "닉네임"));
    }
}
