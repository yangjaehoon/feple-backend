package com.feple.feple_backend.admin;

import com.feple.feple_backend.artist.service.ArtistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
@RequiredArgsConstructor
class ArtistFilterDropdownProvider implements FilterDropdownProvider {

    private final ArtistService artistService;

    @Override public String filter() { return "ARTIST"; }

    @Override public void populate(Model model) {
        model.addAttribute("artists", artistService.getAllArtistsSortedByName());
    }
}
