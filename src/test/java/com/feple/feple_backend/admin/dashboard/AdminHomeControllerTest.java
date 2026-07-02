package com.feple.feple_backend.admin.dashboard;

import com.feple.feple_backend.admin.AdminDashboardAssembler;
import com.feple.feple_backend.admin.AdminDashboardDto;
import com.feple.feple_backend.admin.account.AdminAccount;
import com.feple.feple_backend.admin.account.AdminAccountService;
import com.feple.feple_backend.admin.log.AdminLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminHomeControllerTest {

    @Mock AdminDashboardAssembler dashboardAssembler;
    @Mock AdminLogService adminLogService;
    @Mock AdminAccountService adminAccountService;

    @InjectMocks AdminHomeController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void 대시보드_조회_뷰와_모델_속성_확인() throws Exception {
        given(dashboardAssembler.assemble()).willReturn(mock(AdminDashboardDto.class));
        given(adminLogService.getRecentLogs()).willReturn(List.of());
        given(adminAccountService.findByUsername("admin")).willReturn(Optional.empty());

        mockMvc.perform(get("/admin")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard/home"))
                .andExpect(model().attributeExists("dashboard", "recentLogs", "actionLabels"));
    }

    @Test
    void 현재_관리자_계정_있으면_currentAdmin_모델에_추가() throws Exception {
        AdminAccount account = mock(AdminAccount.class);
        given(dashboardAssembler.assemble()).willReturn(mock(AdminDashboardDto.class));
        given(adminLogService.getRecentLogs()).willReturn(List.of());
        given(adminAccountService.findByUsername("admin")).willReturn(Optional.of(account));

        mockMvc.perform(get("/admin")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(model().attribute("currentAdmin", account));
    }
}
