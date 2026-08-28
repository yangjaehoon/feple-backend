package com.feple.feple_backend.admin.user;

import com.feple.feple_backend.admin.AdminActionUtils;
import com.feple.feple_backend.admin.AdminConstants;
import com.feple.feple_backend.admin.AdminUrlUtils;
import com.feple.feple_backend.admin.account.AdminPermission;
import com.feple.feple_backend.admin.account.RequiresAdminPermission;
import com.feple.feple_backend.admin.log.AdminAction;
import com.feple.feple_backend.admin.log.AdminLogService;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.service.PointService;
import com.feple.feple_backend.user.service.UserAdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@RequiresAdminPermission(AdminPermission.USERS)
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private static final String FILTER_BANNED = "banned";
    private static final String SORT_REPORTS  = "reports";

    private final UserAdminService userService;
    private final UserDetailAggregationService userDetailAggregationService;
    private final AdminLogService adminLogService;
    private final PointService pointService;

    @GetMapping
    public String listUsers(@ModelAttribute UserListFilter listFilter, Model model) {
        Page<UserResponseDto> users = fetchUsersPage(listFilter);
        List<Long> userIds = users.getContent().stream().map(UserResponseDto::getId).toList();

        model.addAttribute("totalCount", userService.getTotalCount());
        addListModel(model, users, userDetailAggregationService.getListCounts(userIds), listFilter);
        return "admin/user/list";
    }

    private Page<UserResponseDto> fetchUsersPage(UserListFilter listFilter) {
        if (FILTER_BANNED.equals(listFilter.filter()))
            return userService.getBannedUsersPage(listFilter.page(), AdminConstants.LIST_PAGE_SIZE, listFilter.keyword());
        if (SORT_REPORTS.equals(listFilter.sort()))
            return userService.getUsersPageSortedByReports(listFilter.page(), AdminConstants.LIST_PAGE_SIZE, listFilter.keyword());
        return userService.getUsersPage(listFilter.page(), AdminConstants.LIST_PAGE_SIZE, listFilter.keyword());
    }

    @GetMapping("/{id}")
    public String userDetail(@PathVariable Long id,
                             @ModelAttribute UserListFilter listFilter,
                             Model model, RedirectAttributes ra) {
        return AdminActionUtils.tryRender(
                () -> {
                    addDetailModel(model, userDetailAggregationService.getDetail(id));
                    UriComponentsBuilder builder = withFilterAndSort(UriComponentsBuilder.fromPath("/admin/users"), listFilter)
                            .queryParam("page", listFilter.page());
                    AdminUrlUtils.appendIfHasText(builder, "keyword", listFilter.keyword());
                    model.addAttribute("returnUrl", AdminUrlUtils.toEncodedString(builder));
                    model.addAttribute("listFilter", listFilter);
                },
                "admin/user/detail",
                e -> log.error("회원 상세 조회 실패 id={}", id, e),
                "회원 정보를 불러오는 중 오류가 발생했습니다.",
                "redirect:/admin/users",
                ra);
    }

    @PostMapping("/bulk-delete")
    public String bulkDeleteUsers(@RequestParam(required = false) List<Long> ids,
            RedirectAttributes ra) {
        String invalidSelection = AdminActionUtils.requireValidSelection(ids, "redirect:/admin/users", ra);
        if (invalidSelection != null) return invalidSelection;
        AdminActionUtils.tryAction(
                () -> {
                    userService.bulkDeleteUsers(ids);
                    adminLogService.log(AdminAction.USER_BULK_DELETE, "USER", null,
                            "삭제 " + AdminActionUtils.describeIds(ids));
                },
                ids.size() + "명 회원이 삭제되었습니다.",
                e -> log.error("회원 일괄 삭제 실패 ids={}", ids, e),
                AdminConstants.MSG_BULK_DELETE_ERROR,
                ra);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             @ModelAttribute UserListFilter listFilter,
                             RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    String nickname = userService.adminDeleteUser(id);
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

    @PostMapping("/{id}/points/grant")
    public String grantPoints(@PathVariable Long id,
                              @RequestParam int amount,
                              @RequestParam String reason,
                              RedirectAttributes ra) {
        AdminActionUtils.tryAction(
                () -> {
                    pointService.grantByAdmin(id, amount, reason);
                    adminLogService.log(AdminAction.USER_POINT_GRANT, "USER", id, amount + "P / " + reason);
                },
                amount + "P가 지급되었습니다.",
                e -> log.error("포인트 지급 실패 userId={}", id, e),
                "포인트 지급에 실패했습니다.",
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
        model.addAttribute("recentPointLogs", detail.recentPointLogs());
    }

    private String userListRedirect(UserListFilter listFilter) {
        UriComponentsBuilder builder = withFilterAndSort(UriComponentsBuilder.fromPath("/admin/users"), listFilter)
                .queryParam("page", listFilter.page());
        return AdminActionUtils.toRedirect(builder, listFilter.keyword());
    }

    private static String buildListParams(UserListFilter listFilter) {
        UriComponentsBuilder builder = withFilterAndSort(UriComponentsBuilder.newInstance(), listFilter);
        AdminUrlUtils.appendIfHasText(builder, "keyword", listFilter.keyword());
        return AdminUrlUtils.toQueryString(builder);
    }

    // filter/sort는 목록·상세·리다이렉트 URL 조립에서 공통으로 쓰이는 값 (FILTER_BANNED일 때 sort 생략)
    private static UriComponentsBuilder withFilterAndSort(UriComponentsBuilder builder, UserListFilter listFilter) {
        builder.queryParam("filter", listFilter.filter());
        if (!FILTER_BANNED.equals(listFilter.filter())) builder.queryParam("sort", listFilter.sort());
        return builder;
    }
}
