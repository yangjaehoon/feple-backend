package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.event.CertificationApprovedEvent;
import com.feple.feple_backend.certification.event.CertificationRejectedEvent;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.user.service.PointService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FestivalCertificationAdminServiceImplTest {

    @Mock FestivalCertificationRepository certificationRepository;
    @Mock S3PresignService s3PresignService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock PointService pointService;

    @InjectMocks FestivalCertificationAdminServiceImpl adminService;

    // ── getByStatus ──────────────────────────────────────────────────

    @Test
    void getByStatus_null이면_findAll_호출() {
        given(certificationRepository.findAll(any(Pageable.class)))
                .willReturn(Page.empty());

        adminService.getByStatus(null, 0);

        then(certificationRepository).should().findAll(any(Pageable.class));
        then(certificationRepository).should(never()).findByStatus(any(), any());
    }

    @Test
    void getByStatus_상태_지정이면_findByStatus_호출() {
        given(certificationRepository.findByStatus(eq(CertificationStatus.PENDING), any(Pageable.class)))
                .willReturn(Page.empty());

        adminService.getByStatus(CertificationStatus.PENDING, 0);

        then(certificationRepository).should().findByStatus(eq(CertificationStatus.PENDING), any(Pageable.class));
        then(certificationRepository).should(never()).findAll(any(Pageable.class));
    }

    // ── searchByKeyword / getById ────────────────────────────────────

    @Test
    void 키워드로_인증_검색() {
        given(certificationRepository.searchByKeyword(any(), any(), any()))
                .willReturn(Page.empty());

        adminService.searchByKeyword("홍길동", null, 0);

        then(certificationRepository).should().searchByKeyword(any(), any(), any());
    }

    @Test
    void 인증_단건_조회() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(certificationRepository.findWithUserAndFestivalById(1L)).willReturn(Optional.of(cert));

        assertThat(adminService.getById(1L)).isEqualTo(cert);
    }

    // ── approve / reject ─────────────────────────────────────────────

    @Test
    void 인증_승인시_포인트_지급_및_알림_발송() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(cert.getId()).willReturn(10L);
        given(cert.getUserId()).willReturn(1L);
        given(cert.getFestivalTitle()).willReturn("페스티벌");
        given(certificationRepository.findWithUserAndFestivalById(10L)).willReturn(Optional.of(cert));

        adminService.approve(10L, "admin");

        then(cert).should().approve("admin");
        then(pointService).should().addCertApprovedPoint(1L, 10L);
        then(eventPublisher).should().publishEvent(any(CertificationApprovedEvent.class));
    }

    @Test
    void 인증_거절시_알림_발송() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(cert.getUserId()).willReturn(1L);
        given(certificationRepository.findWithUserAndFestivalById(10L)).willReturn(Optional.of(cert));

        adminService.reject(10L, "사진 불명확", "admin");

        then(cert).should().reject("사진 불명확", "admin");
        then(eventPublisher).should().publishEvent(any(CertificationRejectedEvent.class));
    }

    // ── getPendingCount / findNextPendingId / buildPhotoUrl ─────────────

    @Test
    void 대기중인_인증_수_조회() {
        given(certificationRepository.countByStatus(CertificationStatus.PENDING)).willReturn(5L);

        assertThat(adminService.getPendingCount()).isEqualTo(5L);
    }

    @Test
    void 다음_대기_인증ID_조회() {
        given(certificationRepository.findNextPendingIds(any(), any())).willReturn(List.of(20L));

        assertThat(adminService.findNextPendingId(10L)).contains(20L);
    }

    @Test
    void 다음_대기_인증이_없으면_빈값_반환() {
        given(certificationRepository.findNextPendingIds(any(), any())).willReturn(List.of());

        assertThat(adminService.findNextPendingId(10L)).isEmpty();
    }

    @Test
    void 사진_URL_생성() {
        given(s3PresignService.presignGetUrl("photo-key")).willReturn("https://cdn.example.com/photo-key");

        assertThat(adminService.buildPhotoUrl("photo-key")).isEqualTo("https://cdn.example.com/photo-key");
    }
}
