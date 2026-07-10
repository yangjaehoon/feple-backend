package com.feple.feple_backend.admin.user;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.user.UserDetailAggregationService;
import com.feple.feple_backend.admin.user.UserDetailDto;
import com.feple.feple_backend.admin.user.UserListCountsDto;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.UserRole;
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
import java.util.NoSuchElementException;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private static final String FILTER_BANNED = "banned";
    private static final String SORT_REPORTS  = "reports";

    private final UserAdminService userService;
    private final UserDetailAggregationService userDetailAggregationService;
    private final AdminLogService adminLogService;

    @GetMapping
    public String listUsers(@ModelAttribute UserListFilter listFilter, Model model) {
        Page<UserResponseDto> users = fetchUsersPage(listFilter.page(), listFilter.keyword(), listFilter.sort(), listFilter.filter());
        List<Long> userIds = users.getContent().stream().map(UserResponseDto::getId).toList();

        addListModel(model, users, userDetailAggregationService.getListCounts(userIds), listFilter);
        return "admin/user/list";
    }

    private Page<UserResponseDto> fetchUsersPage(int page, String keyword, String sort, String filter) {
        if (FILTER_BANNED.equals(filter))   return userService.getBannedUsersPage(page, AdminConstants.LIST_PAGE_SIZE, keyword);
        if (SORT_REPORTS.equals(sort))    return userService.getUsersPageSortedByReports(page, AdminConstants.LIST_PAGE_SIZE, keyword);
        return userService.getUsersPage(page, AdminConstants.LIST_PAGE_SIZE, keyword);
    }

    @GetMapping("/{id}")
    public String userDetail(@PathVariable Long id,
                             @ModelAttribute UserListFilter listFilter,
                             Model model, RedirectAttributes ra) {
        try {
            addDetailModel(model, userDetailAggregationService.getDetail(id));
            UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/users")
                    .queryParam("filter", listFilter.filter())
                    .queryParam("page", listFilter.page());
            if (!FILTER_BANNED.equals(listFilter.filter())) builder.queryParam("sort", listFilter.sort());
            if (!listFilter.keyword().isBlank()) builder.queryParam("keyword", listFilter.keyword());
            model.addAttribute("returnUrl", builder.build().toUriString());
            model.addAttribute("listFilter", listFilter);
            return "admin/user/detail";
        } catch (NoSuchElementException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/users";
        } catch (Exception e) {
            log.error("회원 상세 조회 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "회원 정보를 불러오는 중 오류가 발생했습니다.");
            return "redirect:/admin/users";
        }
    }

    @PostMapping("/bulk-delete")
    public String bulkDeleteUsers(@RequestParam(required = false) List<Long> ids,
            RedirectAttributes ra) {
        if (ids != null && !ids.isEmpty()) {
            AdminActionUtils.tryAction(
                    () -> {
                        userService.bulkDeleteUsers(ids);
                        adminLogService.log(AdminAction.USER_BULK_DELETE, "USER", null, "총 " + ids.size() + "명");
                    },
                    ids.size() + "명 회원이 삭제되었습니다.",
                    e -> log.error("회원 일괄 삭제 실패 ids={}", ids, e),
                    "일괄 삭제 처리 중 오류가 발생했습니다.",
                    ra);
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             @ModelAttribute UserListFilter listFilter,
                             RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    String nickname = userService.getAdminUser(id).getNickname();
                    userService.adminDeleteUser(id);
                    adminLogService.log(AdminAction.USER_DELETE, "USER", id, nickname);
                },
                "회원이 삭제되었습니다.",
                e -> log.error("회원 삭제 실패 id={}", id, e),
                "회원 삭제에 실패했습니다.",
                ra);
        return userListRedirect(listFilter);
    }

    @PostMapping("/{id}/role")
    public String updateUserRole(@PathVariable Long id,
                                 @RequestParam UserRole role,
                                 RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    userService.updateUserRole(id, role);
                    adminLogService.log(AdminAction.USER_ROLE_CHANGE, "USER", id, role.getDisplayName());
                },
                "역할이 변경되었습니다: " + role.getDisplayName(),
                e -> log.error("회원 역할 변경 실패 id={}", id, e),
                "역할 변경에 실패했습니다.",
                ra);
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/ban")
    public String banUser(@PathVariable Long id,
                          @RequestParam(defaultValue = "7") int days,
                          @RequestParam(required = false) String reason,
                          RedirectAttributes ra) {
        String label = days <= 0 ? "영구" : days + "일";
        String detail = label + " 정지" + (reason != null && !reason.isBlank() ? " / " + reason : "");
        AdminActionUtils.tryAction(
                () -> {
                    userService.banUser(id, days, reason);
                    adminLogService.log(AdminAction.USER_BAN, "USER", id, detail);
                },
                label + " 정지가 적용되었습니다.",
                e -> log.error("회원 정지 처리 중 오류. userId={}", id, e),
                "정지 처리 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/unban")
    public String unbanUser(@PathVariable Long id, RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    userService.unbanUser(id);
                    adminLogService.log(AdminAction.USER_UNBAN, "USER", id, null);
                },
                "정지가 해제되었습니다.",
                e -> log.error("회원 정지 해제 중 오류. userId={}", id, e),
                "정지 해제 중 오류가 발생했습니다.",
                ra);
        return "redirect:/admin/users/" + id;
    }

    private void addListModel(Model model, Page<UserResponseDto> users,
                               UserListCountsDto counts, UserListFilter listFilter) {
        model.addAttribute("users",         users);
        model.addAttribute("keyword",       listFilter.keyword());
        model.addAttribute("sort",          listFilter.sort());
        model.addAttribute("filter",        listFilter.filter());
        model.addAttribute("reportCounts",  counts.reportCounts());
        model.addAttribute("postCounts",    counts.postCounts());
        model.addAttribute("commentCounts", counts.commentCounts());
        model.addAttribute("extraParams",   buildListParams(listFilter));
    }

    private static void addDetailModel(Model model, UserDetailDto detail) {
        model.addAttribute("user",            detail.user());
        model.addAttribute("stats",           detail.stats());
        model.addAttribute("recentPosts",     detail.recentPosts());
        model.addAttribute("recentComments",  detail.recentComments());
        model.addAttribute("likedFestivals",  detail.likedFestivals());
        model.addAttribute("followedArtists", detail.followedArtists());
        model.addAttribute("blockedUsers",    detail.blockedUsers());
        model.addAttribute("certifications",  detail.certifications());
    }

    private String userListRedirect(UserListFilter listFilter) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/users")
                .queryParam("filter", listFilter.filter())
                .queryParam("page", listFilter.page());
        if (!FILTER_BANNED.equals(listFilter.filter())) builder.queryParam("sort", listFilter.sort());
        return AdminActionUtils.toRedirect(builder, listFilter.keyword());
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
