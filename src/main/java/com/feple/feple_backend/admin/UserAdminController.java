package com.feple.feple_backend.admin;

import com.feple.feple_backend.comment.service.CommentService;
import com.feple.feple_backend.post.service.PostAdminService;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.service.MyPageService;
import com.feple.feple_backend.user.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private static final int PAGE_SIZE = 20;

    private final UserAdminService userService;
    private final UserDetailAggregationService userDetailAggregationService;
    private final MyPageService myPageService;
    private final CommentService commentService;
    private final PostAdminService postAdminService;
    private final AdminLogService adminLogService;

    @GetMapping
    public String listUsers(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "") String filter,
            Model model) {
        Page<UserResponseDto> users;
        if ("banned".equals(filter)) {
            users = userService.getBannedUsersPage(page, PAGE_SIZE, keyword);
        } else if ("reports".equals(sort)) {
            users = userService.getUsersPageSortedByReports(page, PAGE_SIZE, keyword);
        } else {
            users = userService.getUsersPage(page, PAGE_SIZE, keyword);
        }

        List<Long> userIds = users.getContent().stream().map(UserResponseDto::getId).toList();
        Map<Long, Long> reportCounts = myPageService.getReportCounts(userIds);
        Map<Long, Long> postCounts = postAdminService.getPostCountsByUserIds(userIds);
        Map<Long, Long> commentCounts = commentService.getCommentCountsByUserIds(userIds);

        String extraParams = buildListParams(filter, sort, keyword);

        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("filter", filter);
        model.addAttribute("reportCounts", reportCounts);
        model.addAttribute("postCounts", postCounts);
        model.addAttribute("commentCounts", commentCounts);
        model.addAttribute("extraParams", extraParams);
        return "admin/user-list";
    }

    @GetMapping("/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        userDetailAggregationService.populateModel(id, model);
        return "admin/user-detail";
    }

    @PostMapping("/bulk-delete")
    public String bulkDeleteUsers(@RequestParam(required = false) List<Long> ids,
            RedirectAttributes ra) {
        if (ids != null && !ids.isEmpty()) {
            userService.bulkDeleteUsers(ids);
            adminLogService.log("USER_BULK_DELETE", "USER", null, "총 " + ids.size() + "명");
            ra.addFlashAttribute("successMessage", ids.size() + "명 회원이 삭제되었습니다.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            String nickname = userService.getAdminUser(id).getNickname();
            userService.adminDeleteUser(id);
            adminLogService.log("USER_DELETE", "USER", id, nickname);
            ra.addFlashAttribute("successMessage", "회원이 삭제되었습니다.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "회원 삭제에 실패했습니다.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/role")
    public String updateUserRole(@PathVariable Long id,
                                 @RequestParam UserRole role,
                                 RedirectAttributes ra) {
        userService.updateUserRole(id, role);
        adminLogService.log("USER_ROLE_CHANGE", "USER", id, role.getDisplayName());
        ra.addFlashAttribute("successMessage", "역할이 변경되었습니다: " + role.getDisplayName());
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/ban")
    public String banUser(@PathVariable Long id,
                          @RequestParam(defaultValue = "7") int days,
                          @RequestParam(required = false) String reason,
                          RedirectAttributes ra) {
        try {
            userService.banUser(id, days, reason);
            String label = days <= 0 ? "영구" : days + "일";
            String detail = label + " 정지" + (reason != null && !reason.isBlank() ? " / " + reason : "");
            adminLogService.log("USER_BAN", "USER", id, detail);
            ra.addFlashAttribute("successMessage", label + " 정지가 적용되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("회원 정지 처리 중 오류. userId={}", id, e);
            ra.addFlashAttribute("errorMessage", "정지 처리 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/unban")
    public String unbanUser(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.unbanUser(id);
            adminLogService.log("USER_UNBAN", "USER", id, null);
            ra.addFlashAttribute("successMessage", "정지가 해제되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("회원 정지 해제 중 오류. userId={}", id, e);
            ra.addFlashAttribute("errorMessage", "정지 해제 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/users/" + id;
    }

    private static String buildListParams(String filter, String sort, String keyword) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .queryParam("filter", filter);
        if (!"banned".equals(filter)) builder.queryParam("sort", sort);
        if (keyword != null && !keyword.isBlank()) builder.queryParam("keyword", keyword);
        String query = builder.build().toUriString();
        return query.startsWith("?") ? query.substring(1) : query;
    }
}
