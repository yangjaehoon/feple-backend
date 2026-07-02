package com.feple.feple_backend.admin.post;

import com.feple.feple_backend.admin.FilterDropdownProvider;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.post.service.PostAdminService;
import com.feple.feple_backend.post.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.Model;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PostAdminControllerTest {

    @Mock PostService postService;
    @Mock PostAdminService postAdminService;
    @Mock CommentService commentService;
    @Mock AdminLogService adminLogService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new PostAdminController(postService, postAdminService, commentService, adminLogService, List.of())
        ).build();
    }

    // ── GET /admin/posts ──────────────────────────────────────────────────────

    @Test
    void 목록_조회_뷰_이름과_모델_속성_확인() throws Exception {
        given(postAdminService.getPostsForAdmin(any())).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/admin/posts"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/post/list"))
                .andExpect(model().attributeExists("posts", "filter", "keyword", "extraParams"));
    }

    @Test
    void 목록_조회_filter_매칭_provider_populate_호출() throws Exception {
        FilterDropdownProvider provider = mock(FilterDropdownProvider.class);
        given(provider.filter()).willReturn("artist");
        given(postAdminService.getPostsForAdmin(any())).willReturn(new PageImpl<>(List.of()));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new PostAdminController(postService, postAdminService, commentService, adminLogService, List.of(provider))
        ).build();

        mvc.perform(get("/admin/posts").param("filter", "artist"))
                .andExpect(status().isOk());

        then(provider).should().populate(any(Model.class));
    }

    @Test
    void 목록_조회_filter_없으면_provider_populate_미호출() throws Exception {
        FilterDropdownProvider provider = mock(FilterDropdownProvider.class);
        given(provider.filter()).willReturn("artist");
        given(postAdminService.getPostsForAdmin(any())).willReturn(new PageImpl<>(List.of()));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new PostAdminController(postService, postAdminService, commentService, adminLogService, List.of(provider))
        ).build();

        mvc.perform(get("/admin/posts"))
                .andExpect(status().isOk());

        then(provider).should(never()).populate(any());
    }

    // ── GET /admin/posts/{id} ─────────────────────────────────────────────────

    @Test
    void 상세_조회_성공() throws Exception {
        given(postService.getPost(1L)).willReturn(mock(PostResponseDto.class));
        given(commentService.getCommentsByPost(1L, null)).willReturn(List.of());

        mockMvc.perform(get("/admin/posts/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/post/detail"))
                .andExpect(model().attributeExists("post", "comments", "backUrl"));
    }

    @Test
    void 상세_조회_NoSuchElementException_목록으로_리다이렉트() throws Exception {
        given(postService.getPost(99L)).willThrow(new NoSuchElementException("존재하지 않는 게시글"));

        mockMvc.perform(get("/admin/posts/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts"))
                .andExpect(flash().attribute("errorMessage", "존재하지 않는 게시글"));
    }

    @Test
    void 상세_조회_일반_예외_일반_에러메시지() throws Exception {
        given(postService.getPost(1L)).willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(get("/admin/posts/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts"))
                .andExpect(flash().attribute("errorMessage", "게시글 정보를 불러오는 중 오류가 발생했습니다."));
    }

    // ── POST /admin/posts/bulk-delete ─────────────────────────────────────────

    @Test
    void 일괄_삭제_ids_없으면_서비스_미호출() throws Exception {
        mockMvc.perform(post("/admin/posts/bulk-delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/admin/posts*"));

        then(postAdminService).should(never()).bulkDeletePosts(any());
    }

    @Test
    void 일괄_삭제_성공_successMessage_설정() throws Exception {
        mockMvc.perform(post("/admin/posts/bulk-delete")
                        .param("ids", "1", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "2개 게시글이 삭제되었습니다."));

        then(postAdminService).should().bulkDeletePosts(List.of(1L, 2L));
    }

    // ── POST /admin/posts/{id}/delete ─────────────────────────────────────────

    @Test
    void 게시글_삭제_성공() throws Exception {
        mockMvc.perform(post("/admin/posts/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("successMessage", "게시글이 삭제되었습니다."));

        then(postAdminService).should().deletePost(1L);
    }

    @Test
    void 게시글_삭제_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("DB 오류")).given(postAdminService).deletePost(1L);

        mockMvc.perform(post("/admin/posts/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "삭제 중 오류가 발생했습니다."));
    }

    // ── POST /admin/posts/comments/{id}/delete ────────────────────────────────

    @Test
    void 댓글_삭제_성공_successMessage_없음() throws Exception {
        mockMvc.perform(post("/admin/posts/comments/10/delete")
                        .param("postId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts/1"))
                .andExpect(flash().attributeCount(0));
    }

    @Test
    void 댓글_삭제_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(commentService).deleteComment(10L);

        mockMvc.perform(post("/admin/posts/comments/10/delete")
                        .param("postId", "1"))
                .andExpect(flash().attribute("errorMessage", "댓글 삭제 중 오류가 발생했습니다."));
    }

    // ── GET /admin/posts/deleted ──────────────────────────────────────────────

    @Test
    void 삭제된_게시글_목록_조회() throws Exception {
        given(postAdminService.getDeletedPosts(200)).willReturn(List.of());

        mockMvc.perform(get("/admin/posts/deleted"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/post/deleted"))
                .andExpect(model().attributeExists("posts"));
    }

    // ── POST /admin/posts/{id}/restore ────────────────────────────────────────

    @Test
    void 게시글_복구_성공() throws Exception {
        mockMvc.perform(post("/admin/posts/1/restore"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts/deleted"))
                .andExpect(flash().attribute("successMessage", "게시글이 복구되었습니다."));

        then(postAdminService).should().restorePost(1L);
    }

    @Test
    void 게시글_복구_실패_errorMessage_설정() throws Exception {
        willThrow(new RuntimeException("오류")).given(postAdminService).restorePost(1L);

        mockMvc.perform(post("/admin/posts/1/restore"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/posts/deleted"))
                .andExpect(flash().attribute("errorMessage", "복구 중 오류가 발생했습니다."));
    }
}
