package com.feple.feple_backend.admin;

import com.feple.feple_backend.service.CommentService;
import com.feple.feple_backend.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/posts")
@RequiredArgsConstructor
public class PostAdminController {

    private final PostService postService;
    private final CommentService commentService;

    @GetMapping
    public String listPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String filter,
            Model model) {
        model.addAttribute("posts", postService.getPostsForAdmin(page, 20, filter));
        model.addAttribute("filter", filter);
        model.addAttribute("page", page);
        return "admin/post-list";
    }

    @GetMapping("/{id}")
    public String postDetail(@PathVariable Long id, Model model) {
        model.addAttribute("post", postService.getPost(id));
        model.addAttribute("comments", commentService.getCommentsByPost(id));
        return "admin/post-detail";
    }

    @PostMapping("/{id}/delete")
    public String deletePost(@PathVariable Long id,
                             @RequestParam(defaultValue = "") String filter,
                             @RequestParam(defaultValue = "0") int page) {
        postService.deletePost(id);
        return "redirect:/admin/posts?filter=" + filter + "&page=" + page;
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id,
                                @RequestParam Long postId) {
        commentService.deleteComment(id);
        return "redirect:/admin/posts/" + postId;
    }
}
