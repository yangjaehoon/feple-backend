package com.feple.feple_backend.auth.service;

import com.feple.feple_backend.auth.dto.KakaoUserResponseDto;
import com.feple.feple_backend.auth.kakao.KakaoApiClient;
import com.feple.feple_backend.user.NicknameGenerator;
import com.feple.feple_backend.user.entity.AuthProvider;
import com.feple.feple_backend.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KakaoAuthServiceTest {

    @Mock KakaoApiClient kakaoApiClient;
    @Mock NicknameGenerator nicknameGenerator;
    @Mock OAuthUserRegistrationService registrationService;

    private KakaoAuthService kakaoAuthService;

    @BeforeEach
    void setUp() {
        kakaoAuthService = new KakaoAuthService(kakaoApiClient, nicknameGenerator, registrationService);
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

    /** nicknameSupplier/userBuilder를 캡처해 실제로 호출해보고, 그 결과로 만들어진 User를 반환한다. */
    @SuppressWarnings("unchecked")
    private User captureBuiltUser() {
        ArgumentCaptor<Supplier<String>> nicknameCaptor = ArgumentCaptor.forClass(Supplier.class);
        ArgumentCaptor<Function<String, User>> builderCaptor = ArgumentCaptor.forClass(Function.class);
        verify(registrationService).registerOrFind(
                eq(AuthProvider.KAKAO), any(), nicknameCaptor.capture(), builderCaptor.capture());
        String nickname = nicknameCaptor.getValue().get();
        return builderCaptor.getValue().apply(nickname);
    }

    @Test
    void 정상_카카오_사용자_정보로_회원가입_요청() {
        KakaoUserResponseDto response = dto(123L, "a@b.com", "홍길동", "http://img");
        given(kakaoApiClient.getMe("token")).willReturn(Mono.just(response));
        given(nicknameGenerator.sanitize("홍길동", "Kakao123")).willReturn("홍길동");
        given(nicknameGenerator.uniquify("홍길동")).willReturn("홍길동1");
        User expected = User.builder().id(1L).nickname("홍길동1").build();
        given(registrationService.registerOrFind(eq(AuthProvider.KAKAO), eq("123"), any(), any()))
                .willReturn(expected);

        User result = kakaoAuthService.authenticate("token").block();

        assertThat(result).isEqualTo(expected);
        User built = captureBuiltUser();
        assertThat(built.getOauthId()).isEqualTo("123");
        assertThat(built.getEmail()).isEqualTo("a@b.com");
        assertThat(built.getNickname()).isEqualTo("홍길동1");
        assertThat(built.getProvider()).isEqualTo(AuthProvider.KAKAO);
        assertThat(built.getProfileImageUrl()).isEqualTo("http://img");
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
        given(nicknameGenerator.sanitize("KakaoUser", "Kakao456")).willReturn("KakaoUser");
        given(nicknameGenerator.uniquify("KakaoUser")).willReturn("KakaoUser");
        given(registrationService.registerOrFind(any(), any(), any(), any()))
                .willReturn(User.builder().id(2L).build());

        kakaoAuthService.authenticate("token").block();
        captureBuiltUser(); // nicknameSupplier를 실제로 호출해 지연 평가된 sanitize/uniquify를 트리거

        verify(nicknameGenerator).sanitize("KakaoUser", "Kakao456");
    }

    @Test
    void 프로필_이미지가_없으면_null() {
        KakaoUserResponseDto response = dto(789L, "a@b.com", "닉네임", "");
        given(kakaoApiClient.getMe("token")).willReturn(Mono.just(response));
        given(nicknameGenerator.sanitize(any(), any())).willReturn("닉네임");
        given(nicknameGenerator.uniquify(any())).willReturn("닉네임");
        given(registrationService.registerOrFind(any(), any(), any(), any()))
                .willReturn(User.builder().id(3L).build());

        kakaoAuthService.authenticate("token").block();

        User built = captureBuiltUser();
        assertThat(built.getProfileImageUrl()).isNull();
    }
}
