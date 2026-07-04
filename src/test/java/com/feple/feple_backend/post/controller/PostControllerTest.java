package com.feple.feple_backend.post.controller;

import com.feple.feple_backend.file.service.S3PresignService;
import com.feple.feple_backend.file.dto.PresignResult;
import com.feple.feple_backend.global.exception.GlobalExceptionHandler;
import com.feple.feple_backend.post.dto.CursorPage;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.service.PostLikeService;
import com.feple.feple_backend.post.service.PostScrapService;
import com.feple.feple_backend.post.service.PostSearchService;
import com.feple.feple_backend.post.service.PostService;
import com.feple.feple_backend.support.AuthTestHelper;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock PostService postService;
    @Mock PostSearchService postSearchService;
    @Mock PostLikeService postLikeService;
    @Mock PostScrapService postScrapService;
    @Mock S3PresignService s3PresignService;

    @InjectMocks PostController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 게시글_단건_조회() throws Exception {
        PostResponseDto dto = mock(PostResponseDto.class);
        given(postService.getPost(1L)).willReturn(dto);

        mockMvc.perform(get("/posts/1"))
                .andExpect(status().isOk());
    }

    @Test
    void 인기_게시글_조회() throws Exception {
        given(postService.getHotPosts()).willReturn(List.of());

        mockMvc.perform(get("/posts/hot"))
                .andExpect(status().isOk());
    }

    @Test
    void 자유_게시글_목록_조회() throws Exception {
        CursorPage<PostResponseDto> page = new CursorPage<>(List.of(), null, false);
        given(postService.getPostsByBoardTypePaged(any(), isNull(), anyInt())).willReturn(page);

        mockMvc.perform(get("/posts/free"))
                .andExpect(status().isOk());
    }

    @Test
    void 게시글_작성_성공() throws Exception {
        given(postService.createPost(any(), eq(1L))).willReturn(42L);

        mockMvc.perform(post("/posts/free")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"테스트 제목\",\"content\":\"테스트 내용\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void 게시글_좋아요_토글() throws Exception {
        given(postLikeService.toggleLike(1L, 1L)).willReturn(true);

        mockMvc.perform(post("/posts/1/like")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 게시글_삭제_성공() throws Exception {
        mockMvc.perform(delete("/posts/1")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    void 게시글_검색_성공() throws Exception {
        given(postSearchService.searchPosts("테스트", null)).willReturn(List.of());

        mockMvc.perform(get("/posts/search")
                        .param("keyword", "테스트"))
                .andExpect(status().isOk());
    }

    @Test
    void 이미지_업로드_URL_허용된_타입_성공() throws Exception {
        PresignResult result = new PresignResult("https://s3.example.com/upload", "posts/1/image.jpg");
        given(s3PresignService.presignPut(anyString(), eq("image/jpeg"))).willReturn(result);

        mockMvc.perform(post("/posts/image-upload-url")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/jpeg\",\"extension\":\"jpg\"}"))
                .andExpect(status().isOk());
    }
}
