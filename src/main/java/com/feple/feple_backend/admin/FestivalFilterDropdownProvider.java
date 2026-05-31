package com.feple.feple_backend.admin;

import com.feple.feple_backend.festival.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

@Component
@RequiredArgsConstructor
class FestivalFilterDropdownProvider implements FilterDropdownProvider {

    private final FestivalService festivalService;

    @Override public String filter() { return "FESTIVAL"; }

    @Override public void populate(Model model) {
        model.addAttribute("festivals", festivalService.getAllFestivals(null, null, null, true));
    }
}
