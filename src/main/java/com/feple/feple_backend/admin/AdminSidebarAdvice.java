package com.feple.feple_backend.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.feple.feple_backend.admin")
@RequiredArgsConstructor
public class AdminSidebarAdvice {

    private final AdminSidebarCountService sidebarCountService;

    @ModelAttribute
    public void sidebarCounts(Model model) {
        try {
            AdminSidebarCountService.Counts c = sidebarCountService.getCounts();
            model.addAttribute("sidebarReportCount", c.reportCount());
            model.addAttribute("sidebarCertCount", c.certCount());
            model.addAttribute("sidebarSongRequestCount", c.songRequestCount());
            model.addAttribute("sidebarArtistSuggestionCount", c.suggestionCount());
        } catch (Exception e) {
            model.addAttribute("sidebarReportCount", 0L);
            model.addAttribute("sidebarCertCount", 0L);
            model.addAttribute("sidebarSongRequestCount", 0L);
            model.addAttribute("sidebarArtistSuggestionCount", 0L);
        }
    }
}
