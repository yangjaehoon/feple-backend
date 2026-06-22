package com.feple.feple_backend.user.controller;

import com.feple.feple_backend.artist.song.service.SongRequestService;
import com.feple.feple_backend.global.exception.GlobalExceptionHandler;
import com.feple.feple_backend.support.AuthTestHelper;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.service.DeviceTokenService;
import com.feple.feple_backend.user.service.MyPageService;
import com.feple.feple_backend.user.service.UserService;
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

import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock UserService userService;
    @Mock MyPageService myPageService;
    @Mock DeviceTokenService deviceTokenService;
    @Mock SongRequestService songRequestService;

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
        given(userService.checkNicknameAvailable("tester", null)).willReturn(Map.of("available", true));

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
        given(userService.updateNickname(1L, "newnick")).willReturn(dto);

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
    void 사용자_삭제_성공() throws Exception {
        mockMvc.perform(delete("/users/1")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isNoContent());
    }
}
