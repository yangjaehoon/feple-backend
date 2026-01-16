package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.dto.ArtistRequestDto;
import com.feple.feple_backend.artist.service.ArtistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/artists")
@RequiredArgsConstructor
public class ArtistAdminController {

    private final ArtistService artistService;

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("artist", new ArtistRequestDto());
        return "admin/artist-form";
    }

    @PostMapping("/new")
    public String createArtist(@ModelAttribute("artist") ArtistRequestDto dto) {
        artistService.createArtist(dto);
        return "redirect:/admin/artists";
    }

    @GetMapping
    public String listArtists(Model model) {
        model.addAttribute("artists", artistService.getAllArtists());
        return "admin/artists-list";
    }
}
