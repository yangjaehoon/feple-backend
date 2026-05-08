package com.feple.feple_backend.admin;

import com.feple.feple_backend.admin.service.AdminStatsService;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.post.service.PostAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminHomeController {

    private final FestivalService festivalService;
    private final ArtistService artistService;
    private final PostAdminService postAdminService;
    private final AdminStatsService adminStatsService;

    @GetMapping
    public String adminHome(@RequestParam(defaultValue = "0") int festivalPage,
                            @RequestParam(defaultValue = "0") int artistPage,
                            Model model) {

        model.addAttribute("festivalPage", festivalService.getFestivalsPage(festivalPage, 10));
        model.addAttribute("artistPage", artistService.getArtistsPage(artistPage, 10));
        model.addAttribute("totalPosts", postAdminService.getTotalPostCount());
        model.addAttribute("totalUsers", adminStatsService.getTotalUserCount());
        model.addAttribute("recentPostCount", postAdminService.countRecentPosts(7));
        model.addAttribute("hotPosts", postAdminService.getAdminHotPosts(5));
        model.addAttribute("topArtists", artistService.getTopArtists(5));
        model.addAttribute("recentUsers", adminStatsService.getRecentUsers());
        model.addAttribute("dailyStats", adminStatsService.getDailyStats());
        model.addAttribute("pendingCerts", adminStatsService.getPendingCerts(5));
        model.addAttribute("pendingCertCount", adminStatsService.getPendingCertCount());
        model.addAttribute("pendingReports", adminStatsService.getPendingReports(5));
        model.addAttribute("pendingReportCount", adminStatsService.getPendingReportCount());

        return "admin/admin-home";
    }
}
