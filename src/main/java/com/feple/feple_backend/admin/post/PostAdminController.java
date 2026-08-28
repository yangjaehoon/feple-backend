package com.feple.feple_backend.admin.post;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.admin.account.RequiresAdminPermission;
import com.feple.feple_backend.admin.filter.FilterDropdownProvider;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.post.dto.PostAdminFilterDto;
import com.feple.feple_backend.post.service.PostAdminService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@RequiresAdminPermission(AdminPermission.POSTS)
@Controller
@RequestMapping("/admin/posts")
public class PostAdminController {

    private final PostAdminService postAdminService;
    private final CommentService commentService;
    private final AdminLogService adminLogService;
    private final Map<String, FilterDropdownProvider> dropdownProviders;

    public PostAdminController(PostAdminService postAdminService,
                               CommentService commentService,
                               AdminLogService adminLogService,
                               List<FilterDropdownProvider> providers) {
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
        return AdminActionUtils.tryRender(
                () -> {
                    model.addAttribute("post", postAdminService.getPostForAdmin(id));
                    model.addAttribute("comments", commentService.getAdminCommentsByPost(id, AdminConstants.POST_DETAIL_COMMENT_LIMIT));
                    model.addAttribute("backUrl", "/admin/posts?page=" + params.page() + "&" + params.toExtraParams());
                },
                "admin/post/detail",
                e -> log.error("게시글 상세 조회 실패 id={}", id, e),
                "게시글 정보를 불러오는 중 오류가 발생했습니다.",
                "redirect:/admin/posts",
                ra);
    }

    @PostMapping("/bulk-delete")
    public String bulkDeletePosts(@RequestParam(required = false) List<Long> ids,
                                  @ModelAttribute PostListParams params,
                                  RedirectAttributes ra) {
        String redirectUrl = "redirect:/admin/posts?" + params.toRedirectParams();
        String invalidSelection = AdminActionUtils.requireValidSelection(ids, redirectUrl, ra);
        if (invalidSelection != null) return invalidSelection;
        AdminActionUtils.tryAction(
                () -> {
                    postAdminService.bulkDeletePosts(ids);
                    adminLogService.log(AdminAction.POST_BULK_DELETE, "POST", null,
                            "삭제 " + AdminActionUtils.describeIds(ids));
                },
                ids.size() + "개 게시글이 삭제되었습니다.",
                e -> log.error("게시글 일괄 삭제 실패 ids={}", ids, e),
                AdminConstants.MSG_BULK_DELETE_ERROR,
                ra);
        return redirectUrl;
    }

    @PostMapping("/{id}/pin")
    public String togglePin(@PathVariable Long id, RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    boolean pinned = postAdminService.togglePin(id);
                    adminLogService.log(AdminAction.POST_PIN_TOGGLE, "POST", id, "pinned=" + pinned);
                },
                "게시글 고정 상태가 변경되었습니다.",
                e -> log.error("게시글 고정 토글 실패 id={}", id, e),
                "고정 상태 변경 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/posts/" + id;
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
                AdminConstants.MSG_DELETE_ERROR,
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

    @GetMapping("/blinded")
    public String blindedPosts(Model model) {
        model.addAttribute("posts", postAdminService.getBlindedPosts(AdminConstants.BLINDED_POSTS_LIMIT));
        return "admin/post/blinded";
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
                AdminConstants.MSG_RESTORE_ERROR,
                ra);
        return "redirect:/admin/posts/deleted";
    }
}
