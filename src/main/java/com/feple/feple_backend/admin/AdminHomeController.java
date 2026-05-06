package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.comment.repository.CommentRepository;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import com.feple.feple_backend.post.repository.PostReportRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.service.PostService;
import com.feple.feple_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminHomeController {

    private final FestivalService festivalService;
    private final ArtistService artistService;
    private final PostService postService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostReportRepository reportRepository;

    @GetMapping
    public String adminHome(@RequestParam(defaultValue = "0") int festivalPage,
                            @RequestParam(defaultValue = "0") int artistPage,
                            Model model) {

        Page<FestivalResponseDto> festivals =
                festivalService.getFestivalsPage(festivalPage, 10);
        Page<ArtistResponseDto> artists =
                artistService.getArtistsPage(artistPage, 10);

        model.addAttribute("festivalPage", festivals);
        model.addAttribute("artistPage", artists);
        model.addAttribute("totalPosts", postService.getTotalPostCount());
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("recentPostCount", postService.countRecentPosts(7));
        model.addAttribute("hotPosts", postService.getAdminHotPosts(5));
        model.addAttribute("topArtists", artistService.getTopArtists(5));
        model.addAttribute("recentUsers", userRepository.findTop5ByOrderByIdDesc());
        model.addAttribute("dailyStats", buildDailyStats());

        return "admin/admin-home";
    }

    private List<DailyStatDto> buildDailyStats() {
        List<DailyStatDto> stats = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();
            stats.add(new DailyStatDto(
                (date.getMonthValue()) + "/" + date.getDayOfMonth(),
                userRepository.countByCreatedAtBetween(start, end),
                postRepository.countByCreatedAtBetween(start, end),
                commentRepository.countByCreatedAtBetween(start, end),
                reportRepository.countByCreatedAtBetween(start, end)
            ));
        }
        return stats;
    }
}
