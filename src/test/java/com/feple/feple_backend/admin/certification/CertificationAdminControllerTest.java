package com.feple.feple_backend.admin.certification;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.service.FestivalCertificationAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CertificationAdminControllerTest {

    @Mock FestivalCertificationAdminService certificationService;
    @Mock AdminLogService adminLogService;

    @InjectMocks CertificationAdminController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // ── GET /admin/certifications ─────────────────────────────────────────────

    @Test
    void 목록_조회_뷰와_모델_속성_확인() throws Exception {
        given(certificationService.getByStatus(null, 0)).willReturn(new PageImpl<>(List.of()));
        given(certificationService.getPendingCount()).willReturn(0L);

        mockMvc.perform(get("/admin/certifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/certification/list"))
                .andExpect(model().attributeExists("certifications", "status", "page", "pendingCount"));
    }

    @Test
    void keyword_있으면_searchByKeyword_호출() throws Exception {
        given(certificationService.searchByKeyword("홍길동", null, 0)).willReturn(new PageImpl<>(List.of()));
        given(certificationService.getPendingCount()).willReturn(0L);

        mockMvc.perform(get("/admin/certifications").param("keyword", "홍길동").param("page", "0"))
                .andExpect(status().isOk());

        then(certificationService).should().searchByKeyword("홍길동", null, 0);
        then(certificationService).should(never()).getByStatus(any(), anyInt());
    }

    // ── GET /admin/certifications/{id} ────────────────────────────────────────

    @Test
    void 상세_조회_성공() throws Exception {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(cert.getPhotoKey()).willReturn("photo-key");
        given(certificationService.getById(1L)).willReturn(cert);
        given(certificationService.buildPhotoUrl("photo-key")).willReturn("https://cdn.example.com/photo.jpg");

        mockMvc.perform(get("/admin/certifications/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/certification/detail"))
                .andExpect(model().attributeExists("cert", "photoUrl"));
    }

    @Test
    void 상세_조회_예외_목록으로_리다이렉트() throws Exception {
        given(certificationService.getById(99L)).willThrow(new NoSuchElementException("없는 인증"));

        mockMvc.perform(get("/admin/certifications/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/certifications"))
                .andExpect(flash().attribute("errorMessage", "없는 인증"));
    }

    // ── POST /admin/certifications/{id}/approve ───────────────────────────────

    @Test
    void 승인_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/certifications/1/approve")
                        .param("page", "0")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "인증이 승인되었습니다."));

        then(certificationService).should().approve(1L, "admin");
    }

    @Test
    void 승인_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(certificationService).approve(anyLong(), anyString());

        mockMvc.perform(post("/admin/certifications/1/approve")
                        .param("page", "0")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(flash().attribute("errorMessage", "승인 처리 중 오류가 발생했습니다."));
    }

    // ── POST /admin/certifications/{id}/reject ────────────────────────────────

    @Test
    void 거절_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/certifications/1/reject")
                        .param("rejectionMessage", "사진 불명확")
                        .param("page", "0")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "인증이 거절되었습니다."));

        then(certificationService).should().reject(1L, "사진 불명확", "admin");
    }

    @Test
    void 거절_listRedirect_반환() throws Exception {
        mockMvc.perform(post("/admin/certifications/1/reject")
                        .param("status", "PENDING").param("page", "2")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(redirectedUrl("/admin/certifications?status=PENDING&page=2"));
    }

    // ── POST /admin/certifications/bulk-approve ───────────────────────────────

    @Test
    void 일괄_승인_ids_없으면_errorMessage_선택된_항목_없음() throws Exception {
        mockMvc.perform(post("/admin/certifications/bulk-approve")
                        .param("page", "0")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "선택된 항목이 없습니다."));

        then(certificationService).should(never()).bulkApprove(any(), any());
    }

    @Test
    void 일괄_승인_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/certifications/bulk-approve")
                        .param("ids", "1", "2", "3")
                        .param("page", "0")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "3건이 승인되었습니다."));

        then(certificationService).should().bulkApprove(List.of(1L, 2L, 3L), "admin");
    }

    @Test
    void 일괄_승인_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("처리 오류")).given(certificationService).bulkApprove(any(), any());

        mockMvc.perform(post("/admin/certifications/bulk-approve")
                        .param("ids", "1", "2")
                        .param("page", "0")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(flash().attribute("errorMessage", "일괄 승인 처리 중 오류가 발생했습니다."));
    }

    // ── POST /admin/certifications/bulk-reject ────────────────────────────────

    @Test
    void 일괄_거절_ids_없으면_errorMessage_선택된_항목_없음() throws Exception {
        mockMvc.perform(post("/admin/certifications/bulk-reject")
                        .param("page", "0")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "선택된 항목이 없습니다."));

        then(certificationService).should(never()).bulkReject(any(), any(), any());
    }

    @Test
    void 일괄_거절_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/certifications/bulk-reject")
                        .param("ids", "1", "2")
                        .param("rejectionMessage", "사진 불명확")
                        .param("page", "0")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "2건이 거절되었습니다."));

        then(certificationService).should().bulkReject(List.of(1L, 2L), "사진 불명확", "admin");
    }

    @Test
    void 일괄_거절_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("처리 오류")).given(certificationService).bulkReject(any(), any(), any());

        mockMvc.perform(post("/admin/certifications/bulk-reject")
                        .param("ids", "1")
                        .param("page", "0")
                        .principal(new UsernamePasswordAuthenticationToken("admin", null)))
                .andExpect(flash().attribute("errorMessage", "일괄 거절 처리 중 오류가 발생했습니다."));
    }
}
