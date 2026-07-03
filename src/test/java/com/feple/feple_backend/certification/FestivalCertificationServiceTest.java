package com.feple.feple_backend.certification;

import com.feple.feple_backend.artist.service.S3PresignService;
import com.feple.feple_backend.certification.dto.CertificationResponseDto;
import com.feple.feple_backend.certification.entity.CertificationStatus;
import com.feple.feple_backend.certification.entity.FestivalCertification;
import com.feple.feple_backend.certification.repository.FestivalCertificationRepository;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.festival.entity.Festival;
import com.feple.feple_backend.festival.repository.FestivalRepository;
import com.feple.feple_backend.global.exception.ConflictException;
import com.feple.feple_backend.notification.service.NotificationService;
import com.feple.feple_backend.user.entity.User;
import com.feple.feple_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FestivalCertificationServiceTest {

    @Mock FestivalCertificationRepository certificationRepository;
    @Mock UserRepository userRepository;
    @Mock FestivalRepository festivalRepository;
    @Mock S3PresignService s3PresignService;
    @Mock NotificationService notificationService;

    @InjectMocks FestivalCertificationService festivalCertificationService;

    private static final Long USER_ID = 1L;
    private static final Long FESTIVAL_ID = 2L;
    private static final String VALID_PHOTO_KEY = "certifications/1/photo.jpg";

    @Test
    void submit_잘못된_키_예외() {
        assertThatThrownBy(() ->
                festivalCertificationService.submit(USER_ID, FESTIVAL_ID, "wrong/key"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 오브젝트 키입니다.");
    }

    @Test
    void submit_null_키_예외() {
        assertThatThrownBy(() ->
                festivalCertificationService.submit(USER_ID, FESTIVAL_ID, null))
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
                festivalCertificationService.submit(USER_ID, FESTIVAL_ID, VALID_PHOTO_KEY))
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
                festivalCertificationService.submit(USER_ID, FESTIVAL_ID, VALID_PHOTO_KEY);

        then(certificationRepository).should().saveAndFlush(any(FestivalCertification.class));
        then(s3PresignService).should().presignGetUrl(VALID_PHOTO_KEY);
    }

    @Test
    void getByStatus_null이면_findAll_호출() {
        given(certificationRepository.findAll(any(Pageable.class)))
                .willReturn(Page.empty());

        festivalCertificationService.getByStatus(null, 0);

        then(certificationRepository).should().findAll(any(Pageable.class));
        then(certificationRepository).should(never()).findByStatus(any(), any());
    }

    @Test
    void getByStatus_상태_지정이면_findByStatus_호출() {
        given(certificationRepository.findByStatus(eq(CertificationStatus.PENDING), any(Pageable.class)))
                .willReturn(Page.empty());

        festivalCertificationService.getByStatus(CertificationStatus.PENDING, 0);

        then(certificationRepository).should().findByStatus(eq(CertificationStatus.PENDING), any(Pageable.class));
        then(certificationRepository).should(never()).findAll(any(Pageable.class));
    }

    @Test
    void getCertState_인증_있으면_상태명_반환() {
        FestivalCertification cert = mock(FestivalCertification.class);
        given(cert.getStatus()).willReturn(CertificationStatus.APPROVED);
        given(certificationRepository.findByUserIdAndFestivalId(USER_ID, FESTIVAL_ID))
                .willReturn(Optional.of(cert));

        String result = festivalCertificationService.getCertState(USER_ID, FESTIVAL_ID);

        assertThat(result).isEqualTo("APPROVED");
    }

    @Test
    void getCertState_인증_없으면_NONE_반환() {
        given(certificationRepository.findByUserIdAndFestivalId(USER_ID, FESTIVAL_ID))
                .willReturn(Optional.empty());

        String result = festivalCertificationService.getCertState(USER_ID, FESTIVAL_ID);

        assertThat(result).isEqualTo("NONE");
    }
}
