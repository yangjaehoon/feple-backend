package com.feple.feple_backend.user.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.feple.feple_backend.artist.song.service.SongRequestService;
import com.feple.feple_backend.certification.service.FestivalCertificationService;
import com.feple.feple_backend.global.exception.GlobalExceptionHandler;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.support.AuthTestHelper;
import com.feple.feple_backend.user.dto.NicknameAvailabilityResponse;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.dto.UserStatsDto;
import com.feple.feple_backend.user.entity.DeviceTokenRegistration;
import com.feple.feple_backend.user.service.DeviceTokenService;
import com.feple.feple_backend.user.service.MyPageService;
import com.feple.feple_backend.user.service.UserService;
import com.feple.feple_backend.userblock.service.UserBlockService;
import java.util.List;
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
class UserControllerTest {

    @Mock UserService userService;
    @Mock MyPageService myPageService;
    @Mock DeviceTokenService deviceTokenService;
    @Mock SongRequestService songRequestService;
    @Mock FestivalCertificationService certificationService;
    @Mock UserBlockService userBlockService;

    @InjectMocks UserController controller;

    MockMvc mockMvc;

    MockMvc mockMvcWithGlobalHandler;

    @BeforeEach
    void setUp() {
        // GlobalExceptionHandler 없이 구성하면 Spring MVC 기본 예외 처리(400)가 동작한다.
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        // GlobalExceptionHandler가 필요한 테스트(AccessDeniedException → 403 등)에 사용
        mockMvcWithGlobalHandler = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 닉네임_중복확인_성공() throws Exception {
        given(userService.checkNicknameAvailable("tester", null)).willReturn(NicknameAvailabilityResponse.ok());

        mockMvc.perform(get("/users/check-nickname")
                        .param("nickname", "tester"))
                .andExpect(status().isOk());
    }

    @Test
    void 닉네임_파라미터_없으면_400() throws Exception {
        // @RequestParam required=true (기본값) → MissingServletRequestParameterException → 400
        // GlobalExceptionHandler 없이 Spring MVC 기본 처리
        mockMvc.perform(get("/users/check-nickname"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 사용자_조회() throws Exception {
        UserResponseDto dto = mock(UserResponseDto.class);
        given(userService.getUser(1L)).willReturn(dto);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk());
    }

    @Test
    void 사용자_수정_성공() throws Exception {
        UserResponseDto dto = mock(UserResponseDto.class);
        given(userService.getUser(1L)).willReturn(dto);

        mockMvc.perform(put("/users/1")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"newnick\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 사용자_수정_본인_아니면_403() throws Exception {
        // GlobalExceptionHandler가 AccessDeniedException을 403으로 변환한다.
        mockMvcWithGlobalHandler.perform(put("/users/1")
                        .with(AuthTestHelper.userAuth(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"newnick\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 디바이스_토큰_등록() throws Exception {
        mockMvc.perform(post("/users/device-token")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc123\",\"platform\":\"android\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 디바이스_토큰_등록시_platform_없으면_기본값_사용_language는_그대로_전달() throws Exception {
        // language 기본값("ko")은 UserDeviceToken이 유일한 출처 — 컨트롤러는 null을 그대로 넘긴다
        mockMvc.perform(post("/users/device-token")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc123\"}"))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(deviceTokenService)
                .register(1L, new DeviceTokenRegistration("abc123", "android", null));
    }

    @Test
    void 디바이스_토큰_등록시_platform_language_있으면_그대로_사용() throws Exception {
        mockMvc.perform(post("/users/device-token")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc123\",\"platform\":\"ios\",\"language\":\"en\"}"))
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(deviceTokenService)
                .register(1L, new DeviceTokenRegistration("abc123", "ios", "en"));
    }

    @Test
    void 사용자_삭제_성공() throws Exception {
        mockMvc.perform(delete("/users/1")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"RARELY_USED\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 사용자_삭제_본인_아니면_403() throws Exception {
        mockMvcWithGlobalHandler.perform(delete("/users/1")
                        .with(AuthTestHelper.userAuth(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"RARELY_USED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 프로필_이미지_수정_성공() throws Exception {
        UserResponseDto dto = mock(UserResponseDto.class);
        given(userService.getUser(1L)).willReturn(dto);
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile("file", "p.jpg", "image/jpeg", new byte[]{1});

        mockMvc.perform(multipart("/users/1/profile-image")
                        .file(file)
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 프로필_이미지_수정_본인_아니면_403() throws Exception {
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile("file", "p.jpg", "image/jpeg", new byte[]{1});

        mockMvcWithGlobalHandler.perform(multipart("/users/1/profile-image")
                        .file(file)
                        .with(AuthTestHelper.userAuth(2L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 내정보_조회() throws Exception {
        UserResponseDto dto = mock(UserResponseDto.class);
        given(userService.currentUserId()).willReturn(1L);
        given(userService.getUser(1L)).willReturn(dto);

        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk());
    }

    @Test
    void 팔로우한_아티스트_조회() throws Exception {
        given(myPageService.getFollowedArtists(1L)).willReturn(List.of());

        mockMvc.perform(get("/users/1/following")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 팔로우한_아티스트_조회_본인_아니면_403() throws Exception {
        mockMvcWithGlobalHandler.perform(get("/users/1/following")
                        .with(AuthTestHelper.userAuth(2L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 좋아요한_페스티벌_조회() throws Exception {
        given(myPageService.getLikedFestivals(1L)).willReturn(List.of());

        mockMvc.perform(get("/users/1/liked-festivals")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 좋아요한_페스티벌_조회_본인_아니면_403() throws Exception {
        mockMvcWithGlobalHandler.perform(get("/users/1/liked-festivals")
                        .with(AuthTestHelper.userAuth(2L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 사용자_통계_조회() throws Exception {
        given(myPageService.getUserStats(1L)).willReturn(new UserStatsDto(0, 0, 0, 0, 0, 0));

        mockMvc.perform(get("/users/1/stats"))
                .andExpect(status().isOk());
    }

    @Test
    void 사용자_통계_조회시_신고당한_횟수는_응답에_노출되지_않음() throws Exception {
        // 본인 확인 없는 공개 엔드포인트라 신고당한 횟수(모더레이션 민감정보)는 JSON에서 제외되어야 한다
        given(myPageService.getUserStats(1L)).willReturn(new UserStatsDto(0, 0, 99, 0, 0, 0));

        mockMvc.perform(get("/users/1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportCount").doesNotExist());
    }

    @Test
    void 사용자_인증내역_조회() throws Exception {
        given(certificationService.getPublicCertifications(1L)).willReturn(List.of());

        mockMvc.perform(get("/users/1/certifications"))
                .andExpect(status().isOk());
    }

    @Test
    void 게시글_목록_조회() throws Exception {
        given(myPageService.getPublicPostsPaged(org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
                .willReturn(new CursorPage<>(List.of(), null, false));

        mockMvc.perform(get("/users/1/posts"))
                .andExpect(status().isOk());
    }

    @Test
    void 내_댓글_조회() throws Exception {
        given(myPageService.getMyComments(1L)).willReturn(List.of());

        mockMvc.perform(get("/users/1/comments")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 내_댓글_조회_본인_아니면_403() throws Exception {
        mockMvcWithGlobalHandler.perform(get("/users/1/comments")
                        .with(AuthTestHelper.userAuth(2L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 좋아요한_게시글_조회() throws Exception {
        given(myPageService.getLikedPosts(1L)).willReturn(List.of());

        mockMvc.perform(get("/users/1/liked-posts")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 좋아요한_게시글_조회_본인_아니면_403() throws Exception {
        mockMvcWithGlobalHandler.perform(get("/users/1/liked-posts")
                        .with(AuthTestHelper.userAuth(2L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 소개글_수정() throws Exception {
        UserResponseDto dto = mock(UserResponseDto.class);
        given(userService.getUser(1L)).willReturn(dto);

        mockMvc.perform(patch("/users/1/bio")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"안녕하세요\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 소개글_수정_본인_아니면_403() throws Exception {
        mockMvcWithGlobalHandler.perform(patch("/users/1/bio")
                        .with(AuthTestHelper.userAuth(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"안녕하세요\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 신청곡_목록_조회() throws Exception {
        given(songRequestService.getMyAllRequests(1L)).willReturn(List.of());

        mockMvc.perform(get("/users/1/song-requests")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 신청곡_목록_조회_본인_아니면_403() throws Exception {
        mockMvcWithGlobalHandler.perform(get("/users/1/song-requests")
                        .with(AuthTestHelper.userAuth(2L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void 유저_차단() throws Exception {
        mockMvc.perform(post("/users/2/block")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    void 유저_차단해제() throws Exception {
        mockMvc.perform(delete("/users/2/block")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    void 차단목록_조회() throws Exception {
        given(userBlockService.getBlockedUsers(1L)).willReturn(List.of());

        mockMvc.perform(get("/users/blocked")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 디바이스_토큰_삭제() throws Exception {
        mockMvc.perform(delete("/users/device-token")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc123\"}"))
                .andExpect(status().isNoContent());
    }
}
