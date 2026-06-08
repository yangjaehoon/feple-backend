package com.feple.feple_backend.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.feple.feple_backend.admin")
@RequiredArgsConstructor
public class AdminSidebarAdvice {

    private final AdminSidebarCountService sidebarCountService;

    @ModelAttribute("sidebarReportCount")
    public long sidebarReportCount() {
        try {
            return sidebarCountService.getCounts().reportCount();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("sidebarCertCount")
    public long sidebarCertCount() {
        try {
            return sidebarCountService.getCounts().certCount();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("sidebarSongRequestCount")
    public long sidebarSongRequestCount() {
        try {
            return sidebarCountService.getCounts().songRequestCount();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("sidebarArtistSuggestionCount")
    public long sidebarArtistSuggestionCount() {
        try {
            return sidebarCountService.getCounts().suggestionCount();
        } catch (Exception e) {
            return 0;
        }
    }
}
