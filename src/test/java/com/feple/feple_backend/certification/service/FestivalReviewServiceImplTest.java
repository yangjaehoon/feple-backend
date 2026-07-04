package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.certification.repository.ReviewLikeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FestivalReviewServiceImplTest {

    @Mock FestivalCertificationRepository certificationRepository;
    @Mock ReviewLikeRepository reviewLikeRepository;

    @InjectMocks FestivalReviewServiceImpl reviewService;

    private static final Long USER_ID = 1L;
    private static final Long CERT_ID = 10L;
    private static final Long FESTIVAL_ID = 2L;

    // ── submitRating ─────────────────────────────────────────────────

    @Test
    void 본인_인증에_평점_등록_성공() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(cert.getUserId()).willReturn(USER_ID);
        given(cert.isApproved()).willReturn(true);
        given(certificationRepository.findById(CERT_ID)).willReturn(Optional.of(cert));

        reviewService.submitRating(USER_ID, CERT_ID, 5, "좋았어요");

        then(cert).should().rate(5, "좋았어요");
    }

    @Test
    void 타인_인증에_평점_등록시_예외() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(cert.getUserId()).willReturn(2L);
        given(certificationRepository.findById(CERT_ID)).willReturn(Optional.of(cert));

        assertThatThrownBy(() -> reviewService.submitRating(USER_ID, CERT_ID, 5, "리뷰"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인의 인증에만");
    }

    @Test
    void 미승인_인증에_평점_등록시_예외() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(cert.getUserId()).willReturn(USER_ID);
        given(cert.isApproved()).willReturn(false);
        given(certificationRepository.findById(CERT_ID)).willReturn(Optional.of(cert));

        assertThatThrownBy(() -> reviewService.submitRating(USER_ID, CERT_ID, 5, "리뷰"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("승인된 인증에만");
    }

    // ── getAverageRating / getRatingCount ────────────────────────────

    @Test
    void 평균_평점_소수점_첫째자리_반올림() {
        given(certificationRepository.getAverageRatingByFestivalId(FESTIVAL_ID)).willReturn(4.26);

        assertThat(reviewService.getAverageRating(FESTIVAL_ID)).isEqualTo(4.3);
    }

    @Test
    void 평점_없으면_평균_0_반환() {
        given(certificationRepository.getAverageRatingByFestivalId(FESTIVAL_ID)).willReturn(null);

        assertThat(reviewService.getAverageRating(FESTIVAL_ID)).isZero();
    }

    @Test
    void 평점_수_조회() {
        given(certificationRepository.getRatingCountByFestivalId(FESTIVAL_ID)).willReturn(7);

        assertThat(reviewService.getRatingCount(FESTIVAL_ID)).isEqualTo(7);
    }

    // ── getRatingDistribution ────────────────────────────────────────

    @Test
    void 평점_분포_없으면_모두_0() {
        given(certificationRepository.getRatingDistributionByFestivalId(FESTIVAL_ID)).willReturn(List.of());

        Map<Integer, Long> result = reviewService.getRatingDistribution(FESTIVAL_ID);

        assertThat(result).hasSize(5);
        assertThat(result.values()).allMatch(v -> v == 0L);
    }

    @Test
    void 평점_분포_반환() {
        given(certificationRepository.getRatingDistributionByFestivalId(FESTIVAL_ID))
                .willReturn(List.of(new Object[]{5, 3L}, new Object[]{4, 2L}));

        Map<Integer, Long> result = reviewService.getRatingDistribution(FESTIVAL_ID);

        assertThat(result).containsEntry(5, 3L).containsEntry(4, 2L).containsEntry(1, 0L);
    }

    // ── getFestivalReviewsPage ───────────────────────────────────────

    @Test
    void 리뷰_페이지_조회() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(cert.getId()).willReturn(CERT_ID);
        given(certificationRepository.getRatingDistributionByFestivalId(FESTIVAL_ID)).willReturn(List.of());
        given(certificationRepository.findReviewsByFestivalId(any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(cert)));

        Map<String, Object> result = reviewService.getFestivalReviewsPage(FESTIVAL_ID, 0, USER_ID);

        assertThat(result).containsKeys("averageRating", "ratingCount", "distribution", "reviews", "totalPages", "hasNext");
    }

    // ── toggleReviewLike ─────────────────────────────────────────────

    @Test
    void 리뷰_좋아요_토글_등록() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(cert.isApproved()).willReturn(true);
        given(cert.getRating()).willReturn(5);
        given(certificationRepository.findById(CERT_ID)).willReturn(Optional.of(cert));
        given(reviewLikeRepository.deleteByUserIdAndCertificationId(USER_ID, CERT_ID)).willReturn(0);

        boolean result = reviewService.toggleReviewLike(USER_ID, CERT_ID);

        assertThat(result).isTrue();
        then(certificationRepository).should().incrementLikeCount(CERT_ID);
    }

    @Test
    void 리뷰_좋아요_토글_취소() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(cert.isApproved()).willReturn(true);
        given(cert.getRating()).willReturn(5);
        given(certificationRepository.findById(CERT_ID)).willReturn(Optional.of(cert));
        given(reviewLikeRepository.deleteByUserIdAndCertificationId(USER_ID, CERT_ID)).willReturn(1);

        boolean result = reviewService.toggleReviewLike(USER_ID, CERT_ID);

        assertThat(result).isFalse();
        then(certificationRepository).should().decrementLikeCount(CERT_ID);
    }

    @Test
    void 리뷰가_아니면_좋아요_토글시_예외() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(cert.isApproved()).willReturn(true);
        given(cert.getRating()).willReturn(null);
        given(certificationRepository.findById(CERT_ID)).willReturn(Optional.of(cert));

        assertThatThrownBy(() -> reviewService.toggleReviewLike(USER_ID, CERT_ID))
                .isInstanceOf(IllegalArgumentException.class);
        then(reviewLikeRepository).should(never()).deleteByUserIdAndCertificationId(any(), any());
    }

    // ── removeReviewLikesByUser ──────────────────────────────────────

    @Test
    void 사용자_리뷰_좋아요_일괄_삭제() {
        reviewService.removeReviewLikesByUser(USER_ID);

        then(reviewLikeRepository).should().deleteByUserId(USER_ID);
        then(reviewLikeRepository).should().deleteByCertificationUserId(USER_ID);
    }
}
