package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.post.service.PostAdminService;
import com.feple.feple_backend.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/posts")
@RequiredArgsConstructor
public class PostAdminController {

    private static final int PAGE_SIZE = 20;

    private final PostService postService;
    private final PostAdminService postAdminService;
    private final CommentService commentService;
    private final AdminLogService adminLogService;

    @GetMapping
    public String listPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String filter,
            @RequestParam(defaultValue = "") String keyword,
            Model model) {
        String extraParams = "filter=" + filter + (keyword != null && !keyword.isBlank() ? "&keyword=" + keyword : "");

        model.addAttribute("posts", postAdminService.getPostsForAdmin(page, PAGE_SIZE, filter, keyword));
        model.addAttribute("filter", filter);
        model.addAttribute("keyword", keyword);
        model.addAttribute("page", page);
        model.addAttribute("extraParams", extraParams);
        return "admin/post-list";
    }

    @GetMapping("/{id}")
    public String postDetail(@PathVariable Long id, Model model) {
        model.addAttribute("post", postService.getPost(id));
        model.addAttribute("comments", commentService.getCommentsByPost(id, null));
        return "admin/post-detail";
    }

    @PostMapping("/bulk-delete")
    public String bulkDeletePosts(@RequestParam(required = false) List<Long> ids,
                                  @RequestParam(defaultValue = "") String filter,
                                  @RequestParam(defaultValue = "0") int page,
                                  RedirectAttributes ra) {
        if (ids != null && !ids.isEmpty()) {
            postAdminService.bulkDeletePosts(ids);
            adminLogService.log("POST_BULK_DELETE", "POST", null, "총 " + ids.size() + "개");
            ra.addFlashAttribute("successMessage", ids.size() + "개 게시글이 삭제되었습니다.");
        }
        return "redirect:/admin/posts?filter=" + filter + "&page=" + page;
    }

    @PostMapping("/{id}/delete")
    public String deletePost(@PathVariable Long id,
                             @RequestParam(defaultValue = "") String filter,
                             @RequestParam(defaultValue = "0") int page) {
        postAdminService.deletePost(id);
        adminLogService.log("POST_DELETE", "POST", id, null);
        return "redirect:/admin/posts?filter=" + filter + "&page=" + page;
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id,
                                @RequestParam Long postId) {
        commentService.deleteComment(id);
        adminLogService.log("COMMENT_DELETE", "COMMENT", id, null);
        return "redirect:/admin/posts/" + postId;
    }
}
