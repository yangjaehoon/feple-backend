package com.feple.feple_backend.artistfollow.controller;

import com.feple.feple_backend.artistfollow.dto.FollowResponseDto;
import com.feple.feple_backend.artistfollow.dto.FollowStatusDto;
import com.feple.feple_backend.artistfollow.service.ArtistFollowService;
import com.feple.feple_backend.support.AuthTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArtistFollowControllerTest {

    @Mock ArtistFollowService artistFollowService;

    @InjectMocks ArtistFollowController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void 팔로우_성공() throws Exception {
        FollowResponseDto response = new FollowResponseDto(true, 10);
        given(artistFollowService.follow(1L, 1L)).willReturn(response);

        mockMvc.perform(post("/artists/1/follow")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 언팔로우_성공() throws Exception {
        FollowResponseDto response = new FollowResponseDto(false, 9);
        given(artistFollowService.unfollow(1L, 1L)).willReturn(response);

        mockMvc.perform(delete("/artists/1/follow")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 팔로우_상태_조회() throws Exception {
        FollowStatusDto statusDto = new FollowStatusDto(true, 10);
        given(artistFollowService.followStatus(1L, 1L)).willReturn(statusDto);

        mockMvc.perform(get("/artists/1/follow")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }
}
