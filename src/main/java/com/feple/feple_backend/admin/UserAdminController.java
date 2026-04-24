package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.post.dto.PostResponseDto;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.user.dto.UserResponseDto;
import com.feple.feple_backend.user.dto.UserStatsDto;
import com.feple.feple_backend.user.entity.UserRole;
import com.feple.feple_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserService userService;

    @GetMapping
    public String listUsers(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String keyword,
            Model model) {
        Page<UserResponseDto> users = userService.getUsersPage(page, 20, keyword);
        model.addAttribute("users", users);
        model.addAttribute("keyword", keyword);
        return "admin/user-list";
    }

    @GetMapping("/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        UserResponseDto user = userService.getAdminUser(id);
        UserStatsDto stats = userService.getUserStats(id);
        List<PostResponseDto> recentPosts = userService.getMyPosts(id).stream()
                .limit(10)
                .collect(Collectors.toList());
        List<FestivalResponseDto> likedFestivals = userService.getLikedFestivals(id);
        List<ArtistResponseDto> followedArtists = userService.getFollowedArtists(id);

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("recentPosts", recentPosts);
        model.addAttribute("likedFestivals", likedFestivals);
        model.addAttribute("followedArtists", followedArtists);
        return "admin/user-detail";
    }

    @PostMapping("/bulk-delete")
    public String bulkDeleteUsers(@RequestParam(required = false) List<Long> ids,
            RedirectAttributes ra) {
        if (ids != null && !ids.isEmpty()) {
            userService.bulkDeleteUsers(ids);
            ra.addFlashAttribute("successMessage", ids.size() + "명 회원이 삭제되었습니다.");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        userService.adminDeleteUser(id);
        ra.addFlashAttribute("successMessage", "회원이 삭제되었습니다.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/role")
    public String updateUserRole(@PathVariable Long id,
                                 @RequestParam UserRole role,
                                 RedirectAttributes ra) {
        userService.updateUserRole(id, role);
        ra.addFlashAttribute("successMessage", "역할이 변경되었습니다: " + role.name());
        return "redirect:/admin/users/" + id;
    }
}
