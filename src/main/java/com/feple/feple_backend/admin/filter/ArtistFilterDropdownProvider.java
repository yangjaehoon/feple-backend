package com.feple.feple_backend.admin.filter;

import com.feple.feple_backend.artist.service.ArtistAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
@RequiredArgsConstructor
class ArtistFilterDropdownProvider implements FilterDropdownProvider {

    private final ArtistAdminService artistService;

    @Override public String filterKey() { return "ARTIST"; }

    @Override public void populate(Model model) {
        model.addAttribute("artists", artistService.getAllArtistsSortedByName());
    }
}
