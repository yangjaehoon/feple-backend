package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.dto.ArtistResponseDto;
import com.feple.feple_backend.artist.service.ArtistService;
import com.feple.feple_backend.festival.dto.FestivalResponseDto;
import com.feple.feple_backend.festival.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminHomeController {

    private final FestivalService festivalService;
    private final ArtistService artistService;

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

        return "admin/admin-home";
    }
}
