package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLog;
import com.feple.feple_backend.admin.log.AdminLogFilter;
import com.feple.feple_backend.admin.log.AdminLogRepository;
import com.feple.feple_backend.admin.log.AdminLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminLogServiceTest {

    @Mock AdminLogRepository repository;

    @InjectMocks AdminLogService adminLogService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void log_인증된_사용자명_저장() {
        // 3인자 생성자를 사용해야 isAuthenticated()=true인 토큰이 생성됨
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin", "pass",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        adminLogService.log(AdminAction.FESTIVAL_CREATE, "festival", 1L, "페스티벌 생성");

        ArgumentCaptor<AdminLog> captor = ArgumentCaptor.forClass(AdminLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAdminUsername()).isEqualTo("admin");
    }

    @Test
    void log_인증_없으면_null_저장() {
        adminLogService.log(AdminAction.FESTIVAL_DELETE, "festival", 2L, "페스티벌 삭제");

        ArgumentCaptor<AdminLog> captor = ArgumentCaptor.forClass(AdminLog.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAdminUsername()).isNull();
    }

    @Test
    void getLogs_빈_targetType이면_null_전달() {
        AdminLogFilter filter = new AdminLogFilter("", "", null, null);

        adminLogService.getLogs(0, filter);

        then(repository).should().findWithFilters(
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any(PageRequest.class)
        );
    }

    @Test
    void getLogs_adminUsername_LikeEscape() {
        AdminLogFilter filter = new AdminLogFilter("festival", "ad_min", null, null);

        adminLogService.getLogs(0, filter);

        then(repository).should().findWithFilters(
                eq("festival"),
                eq("ad!_min"),
                isNull(),
                isNull(),
                any(PageRequest.class)
        );
    }

    @Test
    void getLogs_날짜_fromTo_변환() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        AdminLogFilter filter = new AdminLogFilter("", "", from, to);

        adminLogService.getLogs(0, filter);

        LocalDateTime expectedFrom = from.atStartOfDay();
        LocalDateTime expectedTo = to.atTime(LocalTime.MAX);

        then(repository).should().findWithFilters(
                isNull(),
                isNull(),
                eq(expectedFrom),
                eq(expectedTo),
                any(PageRequest.class)
        );
    }

    @Test
    void getRecentLogs_호출() {
        adminLogService.getRecentLogs();

        then(repository).should().findTop10ByOrderByCreatedAtDesc();
    }
}
