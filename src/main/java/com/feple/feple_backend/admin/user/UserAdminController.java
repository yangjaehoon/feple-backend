package com.feple.feple_backend.admin.user;

import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.UserDetailAggregationService;
import com.feple.feple_backend.admin.UserDetailModel;
import com.feple.feple_backend.admin.UserListCountsModel;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private static final String FILTER_BANNED = "banned";

    private final UserAdminService userService;
    private final UserDetailAggregationService userDetailAggregationService;
    private final AdminLogService adminLogService;

    @GetMapping
    @Transactional(readOnly = true)
    public String listUsers(@ModelAttribute UserListFilter listFilter, Model model) {
        Page<UserResponseDto> users = fetchUsersPage(listFilter.page(), listFilter.keyword(), listFilter.sort(), listFilter.filter());
        List<Long> userIds = users.getContent().stream().map(UserResponseDto::getId).toList();

        UserListCountsModel counts = userDetailAggregationService.getListCounts(userIds);
        model.addAttribute("users",         users);
        model.addAttribute("keyword",       listFilter.keyword());
        model.addAttribute("sort",          listFilter.sort());
        model.addAttribute("filter",        listFilter.filter());
        model.addAttribute("reportCounts",  counts.reportCounts());
        model.addAttribute("postCounts",    counts.postCounts());
        model.addAttribute("commentCounts", counts.commentCounts());
        model.addAttribute("extraParams",   buildListParams(listFilter));
        return "admin/user/list";
    }

    private Page<UserResponseDto> fetchUsersPage(int page, String keyword, String sort, String filter) {
        if (FILTER_BANNED.equals(filter))   return userService.getBannedUsersPage(page, AdminConstants.LIST_PAGE_SIZE, keyword);
        if ("reports".equals(sort))    return userService.getUsersPageSortedByReports(page, AdminConstants.LIST_PAGE_SIZE, keyword);
        return userService.getUsersPage(page, AdminConstants.LIST_PAGE_SIZE, keyword);
    }

    @GetMapping("/{id}")
    public String userDetail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            UserDetailModel detail = userDetailAggregationService.getDetail(id);
            model.addAttribute("user",            detail.user());
            model.addAttribute("stats",           detail.stats());
            model.addAttribute("recentPosts",     detail.recentPosts());
            model.addAttribute("recentComments",  detail.recentComments());
            model.addAttribute("likedFestivals",  detail.likedFestivals());
            model.addAttribute("followedArtists", detail.followedArtists());
            return "admin/user/detail";
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users";
        } catch (Exception e) {
            log.error("회원 상세 조회 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "회원 정보를 불러오지 못했습니다.");
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/bulk-delete")
    public String bulkDeleteUsers(@RequestParam(required = false) List<Long> ids,
            RedirectAttributes ra) {
        if (ids != null && !ids.isEmpty()) {
            try {
                userService.bulkDeleteUsers(ids);
                adminLogService.log(AdminAction.USER_BULK_DELETE, "USER", null, "총 " + ids.size() + "명");
                ra.addFlashAttribute("successMessage", ids.size() + "명 회원이 삭제되었습니다.");
            } catch (Exception e) {
                log.error("회원 일괄 삭제 실패 ids={}", ids, e);
                ra.addFlashAttribute("errorMessage", "일괄 삭제 처리 중 오류가 발생했습니다.");
            }
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             @ModelAttribute UserListFilter listFilter,
                             RedirectAttributes ra) {
        try {
            String nickname = userService.getAdminUser(id).getNickname();
            userService.adminDeleteUser(id);
            adminLogService.log(AdminAction.USER_DELETE, "USER", id, nickname);
            ra.addFlashAttribute("successMessage", "회원이 삭제되었습니다.");
        } catch (Exception e) {
            log.error("회원 삭제 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "회원 삭제에 실패했습니다.");
        }
        return userListRedirect(listFilter);
    }

    @PostMapping("/{id}/role")
    public String updateUserRole(@PathVariable Long id,
                                 @RequestParam UserRole role,
                                 RedirectAttributes ra) {
        try {
            userService.updateUserRole(id, role);
            adminLogService.log(AdminAction.USER_ROLE_CHANGE, "USER", id, role.getDisplayName());
            ra.addFlashAttribute("successMessage", "역할이 변경되었습니다: " + role.getDisplayName());
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "역할 변경에 실패했습니다.");
        }
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
            adminLogService.log(AdminAction.USER_BAN, "USER", id, detail);
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
            adminLogService.log(AdminAction.USER_UNBAN, "USER", id, null);
            ra.addFlashAttribute("successMessage", "정지가 해제되었습니다.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            log.error("회원 정지 해제 중 오류. userId={}", id, e);
            ra.addFlashAttribute("errorMessage", "정지 해제 중 오류가 발생했습니다.");
        }
        return "redirect:/admin/users/" + id;
    }

    private String userListRedirect(UserListFilter listFilter) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/users")
                .queryParam("filter", listFilter.filter())
                .queryParam("page", listFilter.page());
        if (!FILTER_BANNED.equals(listFilter.filter())) builder.queryParam("sort", listFilter.sort());
        if (!listFilter.keyword().isBlank()) builder.queryParam("keyword", listFilter.keyword());
        return "redirect:" + builder.build().toUriString();
    }

    private static String buildListParams(UserListFilter listFilter) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
                .queryParam("filter", listFilter.filter());
        if (!FILTER_BANNED.equals(listFilter.filter())) builder.queryParam("sort", listFilter.sort());
        if (!listFilter.keyword().isBlank()) builder.queryParam("keyword", listFilter.keyword());
        String query = builder.build().toUriString();
        return query.startsWith("?") ? query.substring(1) : query;
    }
}
