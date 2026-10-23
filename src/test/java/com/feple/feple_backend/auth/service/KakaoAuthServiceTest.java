package com.feple.feple_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.feple.feple_backend.auth.dto.KakaoUserResponseDto;
import com.feple.feple_backend.auth.kakao.KakaoApiClient;
import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class KakaoAuthServiceTest {

    @Mock KakaoApiClient kakaoApiClient;
    @Mock OAuthUserRegistrationService registrationService;

    private KakaoAuthService kakaoAuthService;

    @BeforeEach
    void setUp() {
        kakaoAuthService = new KakaoAuthService(kakaoApiClient, registrationService);
    }

    private KakaoUserResponseDto dto(Long id, String email, String nickname, String profileImageUrl) {
        KakaoUserResponseDto dto = new KakaoUserResponseDto();
        dto.setId(id);
        KakaoUserResponseDto.KakaoAccount account = new KakaoUserResponseDto.KakaoAccount();
        account.setEmail(email);
        KakaoUserResponseDto.Profile profile = new KakaoUserResponseDto.Profile();
        profile.setNickname(nickname);
        profile.setProfile_image_url(profileImageUrl);
        account.setProfile(profile);
        dto.setKakaoAccount(account);
        return dto;
    }

    private OAuthProfile capturedProfile() {
        ArgumentCaptor<OAuthProfile> captor = ArgumentCaptor.forClass(OAuthProfile.class);
        verify(registrationService).registerOrFind(captor.capture());
        return captor.getValue();
    }

    @Test
    void 정상_카카오_사용자_정보로_회원가입_요청() {
        KakaoUserResponseDto response = dto(123L, "a@b.com", "홍길동", "http://img");
        given(kakaoApiClient.getMe("token")).willReturn(Mono.just(response));
        User expected = User.builder().id(1L).nickname("홍길동1").build();
        given(registrationService.registerOrFind(any())).willReturn(expected);

        User result = kakaoAuthService.authenticate("token").block();

        assertThat(result).isEqualTo(expected);
        assertThat(capturedProfile()).isEqualTo(new OAuthProfile(
                AuthProvider.KAKAO, "123", "a@b.com", "홍길동", "Kakao123", "http://img"));
    }

    @Test
    void 카카오_계정_정보가_없으면_예외() {
        KakaoUserResponseDto response = new KakaoUserResponseDto();
        response.setId(123L);
        given(kakaoApiClient.getMe("token")).willReturn(Mono.just(response));

        assertThatThrownBy(() -> kakaoAuthService.authenticate("token").block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("카카오 계정 정보");
    }

    @Test
    void 카카오_사용자_ID가_없으면_예외() {
        KakaoUserResponseDto response = dto(null, "a@b.com", "닉네임", null);
        given(kakaoApiClient.getMe("token")).willReturn(Mono.just(response));

        assertThatThrownBy(() -> kakaoAuthService.authenticate("token").block())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("카카오 사용자 ID");
    }

    @Test
    void 닉네임이_없으면_KakaoUser로_대체() {
        KakaoUserResponseDto response = dto(456L, "a@b.com", "", null);
        given(kakaoApiClient.getMe("token")).willReturn(Mono.just(response));
        given(registrationService.registerOrFind(any())).willReturn(User.builder().id(2L).build());

        kakaoAuthService.authenticate("token").block();

        OAuthProfile profile = capturedProfile();
        assertThat(profile.rawNickname()).isEqualTo("KakaoUser");
        assertThat(profile.fallbackNickname()).isEqualTo("Kakao456");
    }

    @Test
    void 프로필_이미지가_없으면_null() {
        KakaoUserResponseDto response = dto(789L, "a@b.com", "닉네임", "");
        given(kakaoApiClient.getMe("token")).willReturn(Mono.just(response));
        given(registrationService.registerOrFind(any())).willReturn(User.builder().id(3L).build());

        kakaoAuthService.authenticate("token").block();

        assertThat(capturedProfile().profileImageUrl()).isNull();
    }
}
