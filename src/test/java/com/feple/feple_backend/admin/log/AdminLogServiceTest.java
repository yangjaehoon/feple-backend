package com.feple.feple_backend.admin.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import com.feple.feple_backend.admin.CurrentAdminProvider;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@ExtendWith(MockitoExtension.class)
class AdminLogServiceTest {

    @Mock AdminLogRepository repository;
    @Spy CurrentAdminProvider currentAdminProvider = new CurrentAdminProvider();

    @InjectMocks AdminLogService adminLogService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    // ── log: adminUsername 추출 ──────────────────────────────────────

    @Test
    void 인증된_관리자면_로그에_이름_기록() {
        Authentication auth = new UsernamePasswordAuthenticationToken("admin", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        adminLogService.log(AdminAction.POST_DELETE, "POST", 1L, null);

        ArgumentCaptor<AdminLog> captor = ArgumentCaptor.forClass(AdminLog.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAdminUsername()).isEqualTo("admin");
    }

    @Test
    void 인증정보_없으면_adminUsername_null() {
        adminLogService.log(AdminAction.POST_DELETE, "POST", 1L, null);

        ArgumentCaptor<AdminLog> captor = ArgumentCaptor.forClass(AdminLog.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAdminUsername()).isNull();
    }

    @Test
    void detail이_2000자_이하면_그대로_저장() {
        String detail = "가".repeat(600);

        adminLogService.log(AdminAction.POST_DELETE, "POST", 1L, detail);

        ArgumentCaptor<AdminLog> captor = ArgumentCaptor.forClass(AdminLog.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDetail()).isEqualTo(detail);
    }

    @Test
    void detail이_2000자_초과하면_잘리고_생략_표식_추가() {
        String longDetail = "가".repeat(2500);

        adminLogService.log(AdminAction.POST_DELETE, "POST", 1L, longDetail);

        ArgumentCaptor<AdminLog> captor = ArgumentCaptor.forClass(AdminLog.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        String saved = captor.getValue().getDetail();
        assertThat(saved).hasSize(2000);
        assertThat(saved).endsWith("…(생략)");
        assertThat(saved).startsWith("가".repeat(1995));
    }

    @Test
    void 인증되지_않은_상태면_adminUsername_null() {
        Authentication auth = new UsernamePasswordAuthenticationToken("admin", null, List.of());
        auth.setAuthenticated(false);
        SecurityContextHolder.getContext().setAuthentication(auth);

        adminLogService.log(AdminAction.POST_DELETE, "POST", 1L, null);

        ArgumentCaptor<AdminLog> captor = ArgumentCaptor.forClass(AdminLog.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAdminUsername()).isNull();
    }

    @Test
    void 저장_실패해도_예외_전파하지_않음() {
        willThrow(new RuntimeException("DB 오류")).given(repository).save(any());

        adminLogService.log(AdminAction.POST_DELETE, "POST", 1L, null);
    }

    // ── log: extractClientIp ─────────────────────────────────────────

    @Test
    void 요청_컨텍스트_없으면_ipAddress_null() {
        adminLogService.log(AdminAction.POST_DELETE, "POST", 1L, null);

        ArgumentCaptor<AdminLog> captor = ArgumentCaptor.forClass(AdminLog.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIpAddress()).isNull();
    }

    @Test
    void XForwardedFor_있어도_무시하고_remoteAddr_사용() {
        // 리버스 프록시가 없어 X-Forwarded-For는 클라이언트가 임의로 조작 가능 — 신뢰하지 않는다
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
        request.setRemoteAddr("192.168.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        adminLogService.log(AdminAction.POST_DELETE, "POST", 1L, null);

        ArgumentCaptor<AdminLog> captor = ArgumentCaptor.forClass(AdminLog.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIpAddress()).isEqualTo("192.168.0.1");
    }

    @Test
    void remoteAddr_사용() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        adminLogService.log(AdminAction.POST_DELETE, "POST", 1L, null);

        ArgumentCaptor<AdminLog> captor = ArgumentCaptor.forClass(AdminLog.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        assertThat(captor.getValue().getIpAddress()).isEqualTo("192.168.0.1");
    }

    // ── getLogs ───────────────────────────────────────────────────────

    @Test
    void getLogs_필터_없으면_null로_조회() {
        AdminLogFilter filter = new AdminLogFilter(null, null, null, null, null);
        given(repository.findWithFilters(null, null, null, null, PageRequest.of(0, com.feple.feple_backend.admin.support.AdminConstants.LOG_PAGE_SIZE)))
                .willReturn(new PageImpl<>(List.of()));

        Page<AdminLog> result = adminLogService.getLogs(filter);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getLogs_필터_있으면_해당값으로_조회() {
        AdminLogFilter filter = new AdminLogFilter("POST", "admin", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null);
        given(repository.findWithFilters(
                        org.mockito.ArgumentMatchers.eq("POST"),
                        org.mockito.ArgumentMatchers.eq("admin"),
                        org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 1, 1).atStartOfDay()),
                        org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 1, 31).atTime(java.time.LocalTime.MAX)),
                        any()))
                .willReturn(new PageImpl<>(List.of()));

        Page<AdminLog> result = adminLogService.getLogs(filter);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getLogs_adminUsername는_LIKE_와일드카드가_이스케이프된다() {
        AdminLogFilter filter = new AdminLogFilter("festival", "ad_min", null, null, null);

        adminLogService.getLogs(filter);

        org.mockito.BDDMockito.then(repository).should().findWithFilters(
                org.mockito.ArgumentMatchers.eq("festival"),
                org.mockito.ArgumentMatchers.eq("ad!_min"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(),
                any(PageRequest.class));
    }

    // ── getRecentLogs ─────────────────────────────────────────────────

    @Test
    void getRecentLogs_레포지토리에_위임() {
        given(repository.findTop10ByOrderByCreatedAtDesc()).willReturn(List.of());

        List<AdminLog> result = adminLogService.getRecentLogs();

        assertThat(result).isEmpty();
    }

    @Test
    void countUserBans_USER_BAN_로그_건수를_레포지토리에_위임() {
        given(repository.countByActionAndTargetTypeAndTargetId(AdminAction.USER_BAN, "USER", 7L))
                .willReturn(3L);

        assertThat(adminLogService.countUserBans(7L)).isEqualTo(3L);
    }
}
