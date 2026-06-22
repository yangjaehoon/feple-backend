package com.feple.feple_backend.artist.photo.controller;

import com.feple.feple_backend.artist.photo.dto.ArtistGalleryPhotoResponseDto;
import com.feple.feple_backend.artist.photo.service.ArtistGalleryPhotoService;
import com.feple.feple_backend.artist.photo.service.ArtistPhotoReportService;
import com.feple.feple_backend.file.dto.PresignResult;
import com.feple.feple_backend.global.exception.GlobalExceptionHandler;
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
class ArtistGalleryPhotoControllerTest {

    @Mock ArtistGalleryPhotoService artistGalleryPhotoService;
    @Mock ArtistPhotoReportService artistPhotoReportService;

    @InjectMocks ArtistGalleryPhotoController controller;

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
        PresignResult result = new PresignResult("https://s3.example.com/upload", "artists/1/photo.jpg");
        given(artistGalleryPhotoService.generateUploadUrl(1L, "jpg", "image/jpeg")).willReturn(result);

        mockMvc.perform(post("/artists/1/photos/presign")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentType\":\"image/jpeg\",\"extension\":\"jpg\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 사진_등록_성공() throws Exception {
        ArtistGalleryPhotoResponseDto dto = mock(ArtistGalleryPhotoResponseDto.class);
        given(artistGalleryPhotoService.register(eq(1L), anyString(), anyString(), anyString(), any(), anyBoolean(), eq(1L)))
                .willReturn(dto);

        mockMvc.perform(post("/artists/1/photos")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"objectKey\":\"artists/1/photo.jpg\",\"contentType\":\"image/jpeg\",\"title\":\"사진 제목\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void 사진_목록_조회() throws Exception {
        given(artistGalleryPhotoService.list(1L, 1L)).willReturn(List.of());

        mockMvc.perform(get("/artists/1/photos")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 사진_삭제_성공() throws Exception {
        mockMvc.perform(delete("/artists/1/photos/5")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    void 사진_신고_성공() throws Exception {
        mockMvc.perform(post("/artists/1/photos/5/report")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"SPAM\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void 사진_좋아요_토글() throws Exception {
        given(artistGalleryPhotoService.toggleLike(5L, 1L)).willReturn(true);

        mockMvc.perform(post("/artists/1/photos/5/like")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }
}
