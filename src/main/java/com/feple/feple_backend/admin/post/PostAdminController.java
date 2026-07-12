package com.feple.feple_backend.admin.post;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.filter.FilterDropdownProvider;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.post.dto.PostAdminFilterDto;
import com.feple.feple_backend.post.service.PostAdminService;
import com.feple.feple_backend.post.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/posts")
public class PostAdminController {

    private final PostService postService;
    private final PostAdminService postAdminService;
    private final CommentService commentService;
    private final AdminLogService adminLogService;
    private final Map<String, FilterDropdownProvider> dropdownProviders;

    public PostAdminController(PostService postService,
                               PostAdminService postAdminService,
                               CommentService commentService,
                               AdminLogService adminLogService,
                               List<FilterDropdownProvider> providers) {
        this.postService = postService;
        this.postAdminService = postAdminService;
        this.commentService = commentService;
        this.adminLogService = adminLogService;
        this.dropdownProviders = providers.stream()
                .collect(Collectors.toMap(FilterDropdownProvider::filterKey, p -> p));
    }

    @GetMapping
    public String listPosts(@ModelAttribute PostListParams params, Model model) {
        addListModel(model, postAdminService.getPostsForAdmin(
                new PostAdminFilterDto(params.page(), AdminConstants.LIST_PAGE_SIZE, params.filter(), params.keyword(), params.artistId(), params.festivalId())), params);

        FilterDropdownProvider provider = dropdownProviders.get(params.filter());
        if (provider != null) provider.populate(model);

        return "admin/post/list";
    }

    @GetMapping("/{id}")
    public String postDetail(@PathVariable Long id,
                             @ModelAttribute PostListParams params,
                             Model model,
                             RedirectAttributes ra) {
        try {
            model.addAttribute("post", postService.getPost(id));
            model.addAttribute("comments", commentService.getAdminCommentsByPost(id, AdminConstants.POST_DETAIL_COMMENT_LIMIT));
            model.addAttribute("backUrl", "/admin/posts?page=" + params.page() + "&" + params.toExtraParams());
            return "admin/post/detail";
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/posts";
        } catch (Exception e) {
            log.error("게시글 상세 조회 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "게시글 정보를 불러오는 중 오류가 발생했습니다.");
            return "redirect:/admin/posts";
        }
    }

    @PostMapping("/bulk-delete")
    public String bulkDeletePosts(@RequestParam(required = false) List<Long> ids,
                                  @ModelAttribute PostListParams params,
                                  RedirectAttributes ra) {
        if (ids == null || ids.isEmpty()) {
            ra.addFlashAttribute("errorMessage", AdminConstants.MSG_EMPTY_SELECTION);
        } else {
            AdminActionUtils.tryAction(
                    () -> {
                        postAdminService.bulkDeletePosts(ids);
                        adminLogService.log(AdminAction.POST_BULK_DELETE, "POST", null, "총 " + ids.size() + "개");
                    },
                    ids.size() + "개 게시글이 삭제되었습니다.",
                    e -> log.error("게시글 일괄 삭제 실패 ids={}", ids, e),
                    "일괄 삭제 처리 중 오류가 발생했습니다.",
                    ra);
        }
        return "redirect:/admin/posts?" + params.toRedirectParams();
    }

    @PostMapping("/{id}/delete")
    public String deletePost(@PathVariable Long id,
                             @ModelAttribute PostListParams params,
                             RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    postAdminService.deletePost(id);
                    adminLogService.log(AdminAction.POST_DELETE, "POST", id, null);
                },
                "게시글이 삭제되었습니다.",
                e -> log.error("게시글 삭제 실패 id={}", id, e),
                "삭제 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/posts?" + params.toRedirectParams();
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id,
                                @RequestParam Long postId,
                                RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    commentService.deleteComment(id);
                    adminLogService.log(AdminAction.COMMENT_DELETE, "COMMENT", id, null);
                },
                null,
                e -> log.error("댓글 삭제 실패 id={}", id, e),
                "댓글 삭제 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/posts/" + postId;
    }

    private static void addListModel(Model model, Object posts, PostListParams params) {
        model.addAttribute("posts",       posts);
        model.addAttribute("filter",      params.filter());
        model.addAttribute("keyword",     params.keyword());
        model.addAttribute("artistId",    params.artistId());
        model.addAttribute("festivalId",  params.festivalId());
        model.addAttribute("extraParams", params.toExtraParams());
    }

    @GetMapping("/deleted")
    public String deletedPosts(Model model) {
        model.addAttribute("posts", postAdminService.getDeletedPosts(AdminConstants.DELETED_POSTS_LIMIT));
        return "admin/post/deleted";
    }

    @PostMapping("/{id}/restore")
    public String restorePost(@PathVariable Long id, RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    postAdminService.restorePost(id);
                    adminLogService.log(AdminAction.POST_RESTORE, "POST", id, null);
                },
                "게시글이 복구되었습니다.",
                e -> log.error("게시글 복구 실패 id={}", id, e),
                "복구 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/posts/deleted";
    }
}
