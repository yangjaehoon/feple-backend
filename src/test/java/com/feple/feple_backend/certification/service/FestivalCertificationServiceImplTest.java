package com.feple.feple_backend.certification.service;

import com.feple.feple_backend.certification.dto.CertificationResponseDto;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.file.dto.PresignResult;
import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class FestivalCertificationServiceImplTest {

    @Mock FestivalCertificationRepository certificationRepository;
    @Mock UserRepository userRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock S3PresignService s3PresignService;

    @InjectMocks FestivalCertificationServiceImpl certificationService;

    private static final Long USER_ID = 1L;
    private static final Long FESTIVAL_ID = 2L;
    private static final String VALID_PHOTO_KEY = "certifications/1/photo.jpg";

    // ── submit ───────────────────────────────────────────────────────

    @Test
    void submit_잘못된_키_예외() {
        assertThatThrownBy(() ->
                certificationService.submit(USER_ID, FESTIVAL_ID, "wrong/key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 오브젝트 키입니다.");
    }

    @Test
    void submit_null_키_예외() {
        assertThatThrownBy(() ->
                certificationService.submit(USER_ID, FESTIVAL_ID, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 오브젝트 키입니다.");
    }

    @Test
    void submit_중복_신청_예외() {
        User user = mock(User.class);
        Festival festival = mock(Festival.class);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(festivalRepository.findById(FESTIVAL_ID)).willReturn(Optional.of(festival));
        given(certificationRepository.findByUserIdAndFestivalId(USER_ID, FESTIVAL_ID))
                .willReturn(Optional.of(mock(FestivalCertification.class)));

        assertThatThrownBy(() ->
                certificationService.submit(USER_ID, FESTIVAL_ID, VALID_PHOTO_KEY))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("이미 해당 페스티벌에 인증 신청을 했습니다.");
    }

    @Test
    void submit_성공() {
        User user = mock(User.class);
        Festival festival = mock(Festival.class);
        given(festival.getId()).willReturn(FESTIVAL_ID);
        given(festival.getTitle()).willReturn("페스티벌명");
        given(festival.getPosterKey()).willReturn(null);

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(festivalRepository.findById(FESTIVAL_ID)).willReturn(Optional.of(festival));
        given(certificationRepository.findByUserIdAndFestivalId(USER_ID, FESTIVAL_ID))
                .willReturn(Optional.empty());
        given(s3PresignService.presignGetUrl(VALID_PHOTO_KEY)).willReturn("https://s3.example.com/photo.jpg");

        CertificationResponseDto result =
                certificationService.submit(USER_ID, FESTIVAL_ID, VALID_PHOTO_KEY);

        then(certificationRepository).should().saveAndFlush(any(FestivalCertification.class));
        then(s3PresignService).should().presignGetUrl(VALID_PHOTO_KEY);
    }

    // ── getMyCertifications / getApprovedFestivalIds ────────────────────

    @Test
    void 내_인증_목록_조회() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(cert.getPhotoKey()).willReturn("photo.jpg");
        given(certificationRepository.findByUserId(USER_ID)).willReturn(List.of(cert));

        List<CertificationResponseDto> result = certificationService.getMyCertifications(USER_ID);

        assertThat(result).hasSize(1);
    }

    @Test
    void 승인된_페스티벌ID_목록_조회() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(cert.getFestivalId()).willReturn(FESTIVAL_ID);
        given(certificationRepository.findByUserIdAndStatus(USER_ID, CertificationStatus.APPROVED))
                .willReturn(List.of(cert));

        List<Long> result = certificationService.getApprovedFestivalIds(USER_ID);

        assertThat(result).containsExactly(FESTIVAL_ID);
    }

    // ── countApprovedByUser / getPublicCertifications ───────────────────

    @Test
    void 승인된_인증_수_조회() {
        given(certificationRepository.countByUserIdAndStatus(USER_ID, CertificationStatus.APPROVED))
                .willReturn(3L);

        assertThat(certificationService.countApprovedByUser(USER_ID)).isEqualTo(3L);
    }

    @Test
    void 공개_인증_목록_조회() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(cert.getPhotoKey()).willReturn("photo.jpg");
        given(certificationRepository.findByUserIdAndStatus(USER_ID, CertificationStatus.APPROVED))
                .willReturn(List.of(cert));

        List<CertificationResponseDto> result = certificationService.getPublicCertifications(USER_ID);

        assertThat(result).hasSize(1);
    }

    // ── getCertDetail ────────────────────────────────────────────────

    @Test
    void 인증_상세_없으면_NONE_상태_반환() {
        given(certificationRepository.findByUserIdAndFestivalId(USER_ID, FESTIVAL_ID))
                .willReturn(Optional.empty());

        Map<String, Object> result = certificationService.getCertDetail(USER_ID, FESTIVAL_ID);

        assertThat(result).containsEntry("certState", "NONE");
    }

    @Test
    void 인증_상세_승인상태면_평점정보_포함() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(cert.getStatus()).willReturn(CertificationStatus.APPROVED);
        given(cert.isApproved()).willReturn(true);
        given(cert.getId()).willReturn(10L);
        given(cert.getRating()).willReturn(5);
        given(cert.getUserReview()).willReturn("좋았어요");
        given(certificationRepository.findByUserIdAndFestivalId(USER_ID, FESTIVAL_ID))
                .willReturn(Optional.of(cert));

        Map<String, Object> result = certificationService.getCertDetail(USER_ID, FESTIVAL_ID);

        assertThat(result).containsEntry("certState", "APPROVED")
                .containsEntry("certId", 10L)
                .containsEntry("myRating", 5);
    }

    // ── generateUploadUrl ────────────────────────────────────────────

    @Test
    void 업로드_URL_생성() {
        PresignResult presignResult = new PresignResult("https://s3.example.com/upload", "certifications/1/x.jpg");
        given(s3PresignService.presignPut(any(), any())).willReturn(presignResult);

        PresignResult result = certificationService.generateUploadUrl(USER_ID, "jpg", "image/jpeg");

        assertThat(result).isEqualTo(presignResult);
    }

    // ── findApprovedUserIdsByFestivalId / existsApprovedCertification ───

    @Test
    void 페스티벌_승인_사용자ID_목록_조회() {
        given(certificationRepository.findApprovedUserIdsByFestivalId(FESTIVAL_ID))
                .willReturn(Set.of(USER_ID));

        assertThat(certificationService.findApprovedUserIdsByFestivalId(FESTIVAL_ID)).containsExactly(USER_ID);
    }

    @Test
    void 승인된_인증_존재여부_조회() {
        given(certificationRepository.existsApprovedCertification(FESTIVAL_ID, USER_ID)).willReturn(true);

        assertThat(certificationService.existsApprovedCertification(FESTIVAL_ID, USER_ID)).isTrue();
    }
}
