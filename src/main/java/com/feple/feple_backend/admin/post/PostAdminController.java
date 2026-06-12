package com.feple.feple_backend.admin.post;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.FilterDropdownProvider;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.post.dto.PostAdminFilter;
import com.feple.feple_backend.post.service.PostAdminService;
import com.feple.feple_backend.post.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
                .collect(Collectors.toMap(FilterDropdownProvider::filter, p -> p));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String listPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String filter,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long artistId,
            @RequestParam(required = false) Long festivalId,
            Model model) {
        model.addAttribute("posts", postAdminService.getPostsForAdmin(
                new PostAdminFilter(page, AdminConstants.LIST_PAGE_SIZE, filter, keyword, artistId, festivalId)));
        model.addAttribute("filter", filter);
        model.addAttribute("keyword", keyword);
        model.addAttribute("artistId", artistId);
        model.addAttribute("festivalId", festivalId);
        model.addAttribute("extraParams", new PostListParams(filter, keyword, artistId, festivalId).toExtraParams());

        FilterDropdownProvider provider = dropdownProviders.get(filter);
        if (provider != null) provider.populate(model);

        return "admin/post/list";
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public String postDetail(@PathVariable Long id,
                             @RequestParam(defaultValue = "") String filter,
                             @RequestParam(required = false) Long artistId,
                             @RequestParam(required = false) Long festivalId,
                             Model model,
                             RedirectAttributes ra) {
        try {
            model.addAttribute("post", postService.getPost(id));
            model.addAttribute("comments", commentService.getCommentsByPost(id, null));
            model.addAttribute("backUrl", new PostListParams(filter, null, artistId, festivalId).toBackUrl());
            return "admin/post/detail";
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", "존재하지 않는 게시글입니다.");
            return "redirect:/admin/posts";
        } catch (Exception e) {
            log.error("게시글 상세 조회 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "게시글 정보를 불러오지 못했습니다.");
            return "redirect:/admin/posts";
        }
    }

    @PostMapping("/bulk-delete")
    public String bulkDeletePosts(@RequestParam(required = false) List<Long> ids,
                                  @RequestParam(defaultValue = "") String filter,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(required = false) Long artistId,
                                  @RequestParam(required = false) Long festivalId,
                                  RedirectAttributes ra) {
        if (ids != null && !ids.isEmpty()) {
            try {
                postAdminService.bulkDeletePosts(ids);
                adminLogService.log(AdminAction.POST_BULK_DELETE, "POST", null, "총 " + ids.size() + "개");
                ra.addFlashAttribute("successMessage", ids.size() + "개 게시글이 삭제되었습니다.");
            } catch (Exception e) {
                log.error("게시글 일괄 삭제 실패 ids={}", ids, e);
                ra.addFlashAttribute("errorMessage", "일괄 삭제 처리 중 오류가 발생했습니다.");
            }
        }
        return "redirect:/admin/posts?" + new PostListParams(filter, null, artistId, festivalId).toRedirectParams(page);
    }

    @PostMapping("/{id}/delete")
    public String deletePost(@PathVariable Long id,
                             @RequestParam(defaultValue = "") String filter,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(required = false) Long artistId,
                             @RequestParam(required = false) Long festivalId,
                             RedirectAttributes ra) {
        try {
            postAdminService.deletePost(id);
            adminLogService.log(AdminAction.POST_DELETE, "POST", id, null);
            ra.addFlashAttribute("successMessage", "게시글이 삭제되었습니다.");
        } catch (Exception e) {
            log.error("게시글 삭제 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "삭제 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/posts?" + new PostListParams(filter, null, artistId, festivalId).toRedirectParams(page);
    }

    @PostMapping("/comments/{id}/delete")
    public String deleteComment(@PathVariable Long id,
                                @RequestParam Long postId,
                                RedirectAttributes ra) {
        try {
            commentService.deleteComment(id);
            adminLogService.log(AdminAction.COMMENT_DELETE, "COMMENT", id, null);
        } catch (Exception e) {
            log.error("댓글 삭제 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "댓글 삭제 중 오류가 발생했습니다.");
        }
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
            adminLogService.log(AdminAction.POST_RESTORE, "POST", id, null);
            ra.addFlashAttribute("successMessage", "게시글이 복구되었습니다.");
        } catch (Exception e) {
            log.error("게시글 복구 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "복구 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/posts/deleted";
    }
}
