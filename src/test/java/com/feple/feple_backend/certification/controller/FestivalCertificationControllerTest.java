package com.feple.feple_backend.certification.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.feple.feple_backend.certification.dto.CertificationResponseDto;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.certification.service.FestivalReviewService;
import com.feple.feple_backend.file.dto.S3PresignedUrlResult;
import com.feple.feple_backend.global.exception.GlobalExceptionHandler;
import com.feple.feple_backend.support.AuthTestHelper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FestivalCertificationControllerTest {

    @Mock FestivalCertificationService certificationService;
    @Mock FestivalReviewService reviewService;

    @InjectMocks FestivalCertificationController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void presign_허용된_타입_성공() throws Exception {
        S3PresignedUrlResult result = new S3PresignedUrlResult("https://s3.example.com/upload", "certifications/1/photo.jpg");
        given(certificationService.generateUploadUrl(1L, "jpg", "image/jpeg")).willReturn(result);

        mockMvc.perform(post("/certifications/presign")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/jpeg\",\"extension\":\"jpg\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void presign_허용되지않은_타입_400() throws Exception {
        mockMvc.perform(post("/certifications/presign")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/jpeg\",\"extension\":\"exe\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 인증_신청_성공() throws Exception {
        CertificationResponseDto dto = mock(CertificationResponseDto.class);
        given(certificationService.submit(1L, 2L, "certifications/1/photo.jpg")).willReturn(dto);

        mockMvc.perform(post("/certifications")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"festivalId\":2,\"photoKey\":\"certifications/1/photo.jpg\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void 내_인증_목록_조회() throws Exception {
        given(certificationService.getMyCertifications(1L)).willReturn(List.of());

        mockMvc.perform(get("/certifications")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 인증_상태_조회() throws Exception {
        given(certificationService.getCertDetail(1L, 2L))
                .willReturn(Map.of("certState", "PENDING"));

        mockMvc.perform(get("/certifications/cert-state")
                        .param("festivalId", "2")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certState").value("PENDING"));
    }

    @Test
    void 승인된_페스티벌_ID_목록_조회() throws Exception {
        given(certificationService.getApprovedFestivalIds(1L)).willReturn(List.of(2L, 3L));

        mockMvc.perform(get("/certifications/approved-festivals")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(2))
                .andExpect(jsonPath("$[1]").value(3));
    }

    @Test
    void 별점_등록_성공() throws Exception {
        mockMvc.perform(put("/certifications/1/rating")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"review\":\"최고의 페스티벌\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 별점_범위_초과시_400() throws Exception {
        mockMvc.perform(put("/certifications/1/rating")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 페스티벌_평점_조회() throws Exception {
        given(reviewService.getAverageRating(2L)).willReturn(4.5);
        given(reviewService.getRatingCount(2L)).willReturn(10);

        mockMvc.perform(get("/certifications/festival/2/rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.ratingCount").value(10));
    }

    @Test
    void 페스티벌_리뷰_목록_조회_로그인시_사용자ID_전달() throws Exception {
        given(reviewService.getFestivalReviewsPage(2L, 0, 1L)).willReturn(Map.of("reviews", List.of()));

        mockMvc.perform(get("/certifications/festival/2/reviews")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 페스티벌_리뷰_목록_조회_비로그인이면_null전달() throws Exception {
        given(reviewService.getFestivalReviewsPage(2L, 0, null)).willReturn(Map.of("reviews", List.of()));

        mockMvc.perform(get("/certifications/festival/2/reviews"))
                .andExpect(status().isOk());
    }

    @Test
    void 리뷰_좋아요_토글() throws Exception {
        given(reviewService.toggleReviewLike(1L, 5L)).willReturn(true);

        mockMvc.perform(post("/certifications/5/review-like")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true));
    }
}
