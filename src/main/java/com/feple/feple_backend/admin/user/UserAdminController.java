package com.feple.feple_backend.admin.user;

import com.feple.feple_backend.admin.UserDetailAggregationService;
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

    private static final int PAGE_SIZE = 20;

    private final UserAdminService userService;
    private final UserDetailAggregationService userDetailAggregationService;
    private final AdminLogService adminLogService;

    @GetMapping
    public String listUsers(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "") String filter,
            Model model) {
        Page<UserResponseDto> users = fetchUsersPage(page, keyword, sort, filter);
        List<Long> userIds = users.getContent().stream().map(UserResponseDto::getId).toList();

        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        model.addAttribute("sort", sort);
        model.addAttribute("filter", filter);
        userDetailAggregationService.populateListCountsModel(userIds, model);
        model.addAttribute("extraParams", buildListParams(filter, sort, keyword));
        return "admin/user/list";
    }

    private Page<UserResponseDto> fetchUsersPage(int page, String keyword, String sort, String filter) {
        if ("banned".equals(filter))   return userService.getBannedUsersPage(page, PAGE_SIZE, keyword);
        if ("reports".equals(sort))    return userService.getUsersPageSortedByReports(page, PAGE_SIZE, keyword);
        return userService.getUsersPage(page, PAGE_SIZE, keyword);
    }

    @GetMapping("/{id}")
    public String userDetail(@PathVariable Long id, Model model, RedirectAttributes ra) {
        try {
            userDetailAggregationService.populateModel(id, model);
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
                adminLogService.log("USER_BULK_DELETE", "USER", null, "총 " + ids.size() + "명");
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
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "") String keyword,
                             @RequestParam(defaultValue = "latest") String sort,
                             @RequestParam(defaultValue = "") String filter,
                             RedirectAttributes ra) {
        try {
            String nickname = userService.getAdminUser(id).getNickname();
            userService.adminDeleteUser(id);
            adminLogService.log("USER_DELETE", "USER", id, nickname);
            ra.addFlashAttribute("successMessage", "회원이 삭제되었습니다.");
        } catch (Exception e) {
            log.error("회원 삭제 실패 id={}", id, e);
            ra.addFlashAttribute("errorMessage", "회원 삭제에 실패했습니다.");
        }
        return userListRedirect(filter, sort, page, keyword);
    }

    @PostMapping("/{id}/role")
    public String updateUserRole(@PathVariable Long id,
                                 @RequestParam UserRole role,
                                 RedirectAttributes ra) {
        try {
            userService.updateUserRole(id, role);
            adminLogService.log("USER_ROLE_CHANGE", "USER", id, role.getDisplayName());
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

    private String userListRedirect(String filter, String sort, int page, String keyword) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/admin/users")
                .queryParam("filter", filter != null ? filter : "")
                .queryParam("page", page);
        if (!"banned".equals(filter)) builder.queryParam("sort", sort != null ? sort : "latest");
        if (keyword != null && !keyword.isBlank()) builder.queryParam("keyword", keyword);
        return "redirect:" + builder.build().toUriString();
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
