package com.feple.feple_backend.artist.photo.controller;

import com.feple.feple_backend.artist.photo.service.ArtistProfileImageLikeService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArtistProfileImageControllerTest {

    @Mock ArtistProfileImageLikeService likeService;

    @InjectMocks ArtistProfileImageController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void 아티스트_이미지_좋아요_토글_좋아요됨() throws Exception {
        given(likeService.toggleLike(1L, 1L)).willReturn(true);

        mockMvc.perform(post("/artist-image/1/like")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void 아티스트_이미지_좋아요_토글_좋아요_취소됨() throws Exception {
        given(likeService.toggleLike(1L, 1L)).willReturn(false);

        mockMvc.perform(post("/artist-image/1/like")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }
}
