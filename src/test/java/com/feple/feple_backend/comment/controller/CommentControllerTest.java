package com.feple.feple_backend.comment.controller;

import com.feple.feple_backend.comment.dto.CommentLikeResult;
import com.feple.feple_backend.comment.dto.CommentResponseDto;
import com.feple.feple_backend.comment.service.CommentReportService;
import com.feple.feple_backend.comment.service.CommentService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock CommentService commentService;
    @Mock CommentReportService commentReportService;

    @InjectMocks CommentController controller;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 댓글_생성_성공() throws Exception {
        CommentResponseDto dto = mock(CommentResponseDto.class);
        given(commentService.createComment(any(), eq(1L))).willReturn(dto);

        mockMvc.perform(post("/comments")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":1,\"content\":\"테스트 댓글\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void 댓글_목록_인증없이_조회_가능() throws Exception {
        given(commentService.getCommentsByPost(1L, null)).willReturn(List.of());

        mockMvc.perform(get("/comments/post/1"))
                .andExpect(status().isOk());
    }

    @Test
    void 댓글_삭제_성공() throws Exception {
        mockMvc.perform(delete("/comments/1")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isNoContent());
    }

    @Test
    void 댓글_좋아요_토글() throws Exception {
        CommentLikeResult result = mock(CommentLikeResult.class);
        given(commentService.toggleLike(1L, 1L)).willReturn(result);

        mockMvc.perform(post("/comments/1/like")
                        .with(AuthTestHelper.userAuth(1L)))
                .andExpect(status().isOk());
    }

    @Test
    void 댓글_수정_성공() throws Exception {
        mockMvc.perform(put("/comments/1")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"수정된 내용\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 댓글_신고_성공() throws Exception {
        mockMvc.perform(post("/comments/1/report")
                        .with(AuthTestHelper.userAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"SPAM\"}"))
                .andExpect(status().isCreated());
    }
}
