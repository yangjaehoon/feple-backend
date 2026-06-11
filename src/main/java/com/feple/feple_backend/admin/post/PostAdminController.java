package com.feple.feple_backend.admin.post;

import com.feple.feple_backend.admin.FilterDropdownProvider;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.post.dto.PostAdminFilter;
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
import java.util.stream.Collectors;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/posts")
public class PostAdminController {

    private static final int PAGE_SIZE = 20;

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
                .collect(Collectors.toMap(FilterDropdownProvider::filter, p -> p));
    }

    @GetMapping
    public String listPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String filter,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long artistId,
            @RequestParam(required = false) Long festivalId,
            Model model) {
        StringBuilder extra = new StringBuilder("filter=").append(filter);
        if (keyword != null && !keyword.isBlank()) extra.append("&keyword=").append(keyword);
        if (artistId != null) extra.append("&artistId=").append(artistId);
        if (festivalId != null) extra.append("&festivalId=").append(festivalId);

        model.addAttribute("posts", postAdminService.getPostsForAdmin(
                new PostAdminFilter(page, PAGE_SIZE, filter, keyword, artistId, festivalId)));
        model.addAttribute("filter", filter);
        model.addAttribute("keyword", keyword);
        model.addAttribute("artistId", artistId);
        model.addAttribute("festivalId", festivalId);
        model.addAttribute("extraParams", extra.toString());

        FilterDropdownProvider provider = dropdownProviders.get(filter);
        if (provider != null) provider.populate(model);

        return "admin/post/list";
    }

    @GetMapping("/{id}")
    public String postDetail(@PathVariable Long id,
                             @RequestParam(defaultValue = "") String filter,
                             @RequestParam(required = false) Long artistId,
                             @RequestParam(required = false) Long festivalId,
                             Model model) {
        model.addAttribute("post", postService.getPost(id));
        model.addAttribute("comments", commentService.getCommentsByPost(id, null));
        StringBuilder back = new StringBuilder("/admin/posts");
        if (!filter.isBlank() || artistId != null || festivalId != null) {
            back.append("?filter=").append(filter);
            if (artistId != null) back.append("&artistId=").append(artistId);
            if (festivalId != null) back.append("&festivalId=").append(festivalId);
        }
        model.addAttribute("backUrl", back.toString());
        return "admin/post/detail";
    }

    @PostMapping("/bulk-delete")
    public String bulkDeletePosts(@RequestParam(required = false) List<Long> ids,
                                  @RequestParam(defaultValue = "") String filter,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(required = false) Long artistId,
                                  @RequestParam(required = false) Long festivalId,
                                  RedirectAttributes ra) {
        if (ids != null && !ids.isEmpty()) {
            postAdminService.bulkDeletePosts(ids);
            adminLogService.log("POST_BULK_DELETE", "POST", null, "총 " + ids.size() + "개");
            ra.addFlashAttribute("successMessage", ids.size() + "개 게시글이 삭제되었습니다.");
        }
        return "redirect:/admin/posts?" + buildRedirectParams(filter, page, artistId, festivalId);
    }

    @PostMapping("/{id}/delete")
    public String deletePost(@PathVariable Long id,
                             @RequestParam(defaultValue = "") String filter,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(required = false) Long artistId,
                             @RequestParam(required = false) Long festivalId) {
        postAdminService.deletePost(id);
        adminLogService.log("POST_DELETE", "POST", id, null);
        return "redirect:/admin/posts?" + buildRedirectParams(filter, page, artistId, festivalId);
    }

    private String buildRedirectParams(String filter, int page, Long artistId, Long festivalId) {
        StringBuilder sb = new StringBuilder("filter=").append(filter).append("&page=").append(page);
        if (artistId != null) sb.append("&artistId=").append(artistId);
        if (festivalId != null) sb.append("&festivalId=").append(festivalId);
        return sb.toString();
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id,
                                @RequestParam Long postId) {
        commentService.deleteComment(id);
        adminLogService.log("COMMENT_DELETE", "COMMENT", id, null);
        return "redirect:/admin/posts/" + postId;
    }

    @GetMapping("/deleted")
    public String deletedPosts(Model model) {
        model.addAttribute("posts", postAdminService.getDeletedPosts(200));
        return "admin/post/deleted";
    }

    @PostMapping("/{id}/restore")
    public String restorePost(@PathVariable Long id, RedirectAttributes ra) {
        try {
            postAdminService.restorePost(id);
            adminLogService.log("POST_RESTORE", "POST", id, null);
            ra.addFlashAttribute("successMessage", "게시글이 복구되었습니다.");
        } catch (Exception e) {
            log.error("게시글 복구 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "복구 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/posts/deleted";
    }
}
